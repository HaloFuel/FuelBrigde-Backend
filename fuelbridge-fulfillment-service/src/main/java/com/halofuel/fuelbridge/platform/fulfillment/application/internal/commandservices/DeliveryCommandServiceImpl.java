package com.halofuel.fuelbridge.platform.fulfillment.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.fulfillment.application.commandservices.DeliveryCommandService;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.EquipmentFuelReceipts;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.EquipmentFuelReceipts.FuelReceiptOutcome;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.FuelOrders;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.FuelOrders.FuelOrderSnapshot;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.FuelOrders.OrderingUnavailableException;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.FuelOrders.TransitionOutcome;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.InventoryStock;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.InventoryStock.StockReservationOutcome;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Delivery;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CompleteDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CreateDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.DispatchDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.FailDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DeliveryRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DriverRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.VehicleRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

/**
 * Coordinates deliveries after Fulfillment was extracted from the FuelBridge Platform.
 *
 * <p>Scheduling a delivery is an orchestrated Saga: every local rule is checked first,
 * the stock is reserved in Inventory (compensable step), the local changes are written,
 * and the order dispatch requested to Ordering is the last remote step before the local
 * commit. If Ordering rejects the dispatch, the local changes are rolled back and the
 * reservation is released. If the local commit fails after the dispatch was applied,
 * the dispatch is reverted in Ordering and the reservation released. Completing a delivery registers the fuel receipt in Equipment,
 * requests the receive transition to Ordering and only then saves the local changes.</p>
 */
@Service
public class DeliveryCommandServiceImpl implements DeliveryCommandService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryCommandServiceImpl.class);

    private final DeliveryRepository deliveryRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelOrders fuelOrders;
    private final InventoryStock inventoryStock;
    private final EquipmentFuelReceipts equipmentFuelReceipts;

    public DeliveryCommandServiceImpl(DeliveryRepository deliveryRepository,
                                      DriverRepository driverRepository,
                                      VehicleRepository vehicleRepository,
                                      FuelOrders fuelOrders,
                                      InventoryStock inventoryStock,
                                      EquipmentFuelReceipts equipmentFuelReceipts) {
        this.deliveryRepository = deliveryRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.fuelOrders = fuelOrders;
        this.inventoryStock = inventoryStock;
        this.equipmentFuelReceipts = equipmentFuelReceipts;
    }

    @Override
    @Transactional
    public Result<Delivery, ApplicationError> handle(CreateDeliveryCommand command) {
        var driver = driverRepository.findById(command.driverId());
        var vehicle = vehicleRepository.findById(command.vehicleId());
        if (driver.isEmpty() || vehicle.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Fulfillment resource", "driver or vehicle"));
        }
        if (!command.providerId().equals(driver.get().getProviderId())
                || !command.providerId().equals(vehicle.get().getProviderId())) {
            return Result.failure(ApplicationError.conflict("Delivery",
                    "Driver and vehicle must belong to the selected provider"));
        }
        if (!isAvailable(driver.get().getStatus()) || !isAvailable(vehicle.get().getStatus())) {
            return Result.failure(ApplicationError.conflict("Delivery", "Driver or vehicle is not available"));
        }
        if (deliveryRepository.findByOrderId(command.orderId()).isPresent()) {
            return Result.failure(ApplicationError.conflict("Delivery",
                    "A delivery already exists for order " + command.orderId()));
        }
        Optional<FuelOrderSnapshot> order;
        try {
            order = fuelOrders.findById(command.orderId());
        } catch (OrderingUnavailableException ex) {
            return Result.failure(orderingUnavailable("the delivery was not scheduled"));
        }
        if (order.isEmpty() || !command.providerId().equals(order.get().providerId())) {
            return Result.failure(ApplicationError.notFound("FuelOrder", command.orderId().toString()));
        }
        if (vehicle.get().getCapacity() < order.get().requestedQuantity()) {
            return Result.failure(ApplicationError.conflict("Delivery", "Vehicle capacity is insufficient"));
        }
        // Same rule FuelOrder.dispatch() enforces in Ordering, checked before reserving stock.
        if (!order.get().isPending()) {
            return Result.failure(ApplicationError.conflict("Delivery",
                    "Order " + command.orderId() + " is not pending assignment"));
        }

        // Saga step 1 (compensable): reserve the stock in the Inventory service.
        var fuelProductId = order.get().fuelProductId();
        var quantity = order.get().requestedQuantity();
        var reservation = inventoryStock.reserve(fuelProductId, quantity);
        if (reservation == StockReservationOutcome.INSUFFICIENT_STOCK
                || reservation == StockReservationOutcome.PRODUCT_NOT_FOUND) {
            return Result.failure(ApplicationError.conflict("Delivery", "Insufficient inventory stock"));
        }
        if (reservation == StockReservationOutcome.UNAVAILABLE) {
            return Result.failure(ApplicationError.unexpected("Inventory service",
                    "The stock could not be reserved; the delivery was not scheduled"));
        }
        // Compensation: if the local transaction does not commit, the reserved stock is released.
        var compensatedByTransaction = registerStockCompensation(fuelProductId, quantity);
        try {
            // Saga step 2: local changes, committed only if the dispatch below succeeds.
            driver.get().setStatus("ASSIGNED");
            vehicle.get().setStatus("IN_ROUTE");
            driverRepository.save(driver.get());
            vehicleRepository.save(vehicle.get());
            var delivery = new Delivery(command);
            delivery.dispatch();
            var saved = deliveryRepository.save(delivery);

            // Saga step 3 (pivot): Ordering moves the order from PENDING to DISPATCHED.
            var dispatch = fuelOrders.dispatch(command.orderId());
            if (dispatch != TransitionOutcome.APPLIED) {
                rollbackLocalChanges();
                if (!compensatedByTransaction) {
                    inventoryStock.release(fuelProductId, quantity);
                }
                return Result.failure(transitionError(dispatch, command.orderId(), "dispatch"));
            }
            // Compensation (T10.05): if the local commit fails after Ordering applied the
            // dispatch, the order is returned to PENDING so it can be assigned again.
            registerDispatchCompensation(command.orderId());
            return Result.success(saved);
        } catch (RuntimeException ex) {
            if (!compensatedByTransaction) {
                inventoryStock.release(fuelProductId, quantity);
            }
            throw ex;
        }
    }

    private boolean registerStockCompensation(Long fuelProductId, Double quantity) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    inventoryStock.release(fuelProductId, quantity);
                }
            }
        });
        return true;
    }

    /**
     * Without an active transaction the local changes were already written before the
     * dispatch, so there is no later commit that could fail and nothing to compensate.
     */
    private void registerDispatchCompensation(Long orderId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    return;
                }
                var outcome = fuelOrders.revertDispatch(orderId);
                if (outcome != TransitionOutcome.APPLIED) {
                    log.error("Saga compensation failed: order {} stays DISPATCHED without a delivery (outcome {})",
                            orderId, outcome);
                }
            }
        });
    }

    private static void rollbackLocalChanges() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    @Override
    public Result<Delivery, ApplicationError> handle(DispatchDeliveryCommand command) {
        var existing = deliveryRepository.findById(command.deliveryId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Delivery", command.deliveryId().toString()));
        }
        var delivery = existing.get();
        delivery.dispatch();
        return Result.success(deliveryRepository.save(delivery));
    }

    @Override
    @Transactional
    public Result<Delivery, ApplicationError> handle(CompleteDeliveryCommand command) {
        var existing = deliveryRepository.findById(command.deliveryId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Delivery", command.deliveryId().toString()));
        }
        var delivery = existing.get();
        Optional<FuelOrderSnapshot> order;
        try {
            order = fuelOrders.findById(delivery.getOrderId());
        } catch (OrderingUnavailableException ex) {
            return Result.failure(orderingUnavailable("the delivery was not completed"));
        }
        if (order.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelOrder", delivery.getOrderId().toString()));
        }
        // Same rule FuelOrder.receive() enforces in Ordering, checked before any remote write.
        if (!order.get().isDispatched()) {
            return Result.failure(ApplicationError.conflict("Delivery",
                    "Order " + delivery.getOrderId() + " is not dispatched"));
        }
        // Equipment is a standalone service: register the fuel receipt before saving local changes,
        // so an unavailable service leaves the delivery, order, driver and vehicle untouched.
        if (order.get().equipmentId() != null) {
            var outcome = equipmentFuelReceipts.register(order.get().equipmentId(),
                    order.get().requestedQuantity());
            if (outcome == FuelReceiptOutcome.UNAVAILABLE) {
                return Result.failure(ApplicationError.unexpected("Equipment service",
                        "The fuel receipt could not be registered; the delivery was not completed"));
            }
        }
        var receive = fuelOrders.receive(delivery.getOrderId());
        if (receive != TransitionOutcome.APPLIED) {
            return Result.failure(transitionError(receive, delivery.getOrderId(), "receive"));
        }
        driverRepository.findById(delivery.getDriverId()).ifPresent(driver -> {
            driver.setStatus("AVAILABLE");
            driverRepository.save(driver);
        });
        vehicleRepository.findById(delivery.getVehicleId()).ifPresent(vehicle -> {
            vehicle.setStatus("AVAILABLE");
            vehicleRepository.save(vehicle);
        });
        delivery.complete();
        return Result.success(deliveryRepository.save(delivery));
    }

    @Override
    public Result<Delivery, ApplicationError> handle(FailDeliveryCommand command) {
        var existing = deliveryRepository.findById(command.deliveryId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Delivery", command.deliveryId().toString()));
        }
        var delivery = existing.get();
        delivery.fail(command.reason());
        return Result.success(deliveryRepository.save(delivery));
    }

    private static ApplicationError transitionError(TransitionOutcome outcome, Long orderId, String transition) {
        return switch (outcome) {
            case REJECTED -> ApplicationError.conflict("Delivery",
                    "Ordering rejected the %s of order %d".formatted(transition, orderId));
            case ORDER_NOT_FOUND -> ApplicationError.notFound("FuelOrder", orderId.toString());
            default -> orderingUnavailable("the %s of order %d was not applied".formatted(transition, orderId));
        };
    }

    private static ApplicationError orderingUnavailable(String consequence) {
        return ApplicationError.unexpected("Ordering service",
                "The Ordering service could not be reached; " + consequence);
    }

    private static boolean isAvailable(String status) {
        return "AVAILABLE".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status);
    }
}
