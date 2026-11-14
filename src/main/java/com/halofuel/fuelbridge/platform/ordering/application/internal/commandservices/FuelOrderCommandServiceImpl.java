package com.halofuel.fuelbridge.platform.ordering.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.FuelProductCatalog;
import com.halofuel.fuelbridge.platform.ordering.application.commandservices.FuelOrderCommandService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CancelFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.ConfirmFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CreateFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.DispatchFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.MarkFuelOrderPaidCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.ReceiveFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Coordinates order commands and persistence. Status changes and their domain
 * events belong to the aggregate; handlers invoke its methods before saving.
 * The aggregate guards every transition: confirm, cancel, receive and mark-paid reject an
 * invalid source status with IllegalOrderStateTransitionException (mapped to 409 by
 * OrderingExceptionHandler), while dispatch requires PENDING and rejects other states with a
 * plain IllegalStateException.
 */
@Service
public class FuelOrderCommandServiceImpl implements FuelOrderCommandService {

    private final FuelOrderRepository fuelOrderRepository;
    private final FuelProductCatalog fuelProductCatalog;

    public FuelOrderCommandServiceImpl(FuelOrderRepository fuelOrderRepository,
                                       FuelProductCatalog fuelProductCatalog) {
        this.fuelOrderRepository = fuelOrderRepository;
        this.fuelProductCatalog = fuelProductCatalog;
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(CreateFuelOrderCommand command) {
        var productResult = fuelProductCatalog.findById(command.fuelProductId());
        if (productResult.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelProduct", command.fuelProductId().toString()));
        }
        var product = productResult.get();
        // Capture the catalog price at creation so later price changes do not
        // recalculate the amount stored on this order.
        var totalPrice = product.pricePerUnit() * command.requestedQuantity();
        var order = new FuelOrder(command, totalPrice);
        var saved = fuelOrderRepository.save(order);
        return Result.success(saved);
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(ConfirmFuelOrderCommand command) {
        return updateOrder(command.orderId(), FuelOrder::confirm);
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(CancelFuelOrderCommand command) {
        return updateOrder(command.orderId(), FuelOrder::cancel);
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(DispatchFuelOrderCommand command) {
        return updateOrder(command.orderId(), FuelOrder::dispatch);
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(MarkFuelOrderPaidCommand command) {
        return updateOrderOrConflict(command.orderId(), FuelOrder::markPaid);
    }

    @Override
    public Result<FuelOrder, ApplicationError> handle(ReceiveFuelOrderCommand command) {
        return updateOrderOrConflict(command.orderId(), FuelOrder::receive);
    }

    /**
     * Variant used by the transitions requested by other services (Payment and
     * Fulfillment): a transition the aggregate rejects is returned as a
     * FUELORDER_CONFLICT (409) so the caller can tell it apart from a server error.
     */
    private Result<FuelOrder, ApplicationError> updateOrderOrConflict(Long orderId, Consumer<FuelOrder> transition) {
        var existing = fuelOrderRepository.findById(orderId);
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelOrder", orderId.toString()));
        }
        var order = existing.get();
        try {
            transition.accept(order);
        } catch (IllegalStateException ex) {
            return Result.failure(ApplicationError.conflict("FuelOrder", ex.getMessage()));
        }
        return Result.success(fuelOrderRepository.save(order));
    }

    private Result<FuelOrder, ApplicationError> updateOrder(Long orderId, Consumer<FuelOrder> transition) {
        var existing = fuelOrderRepository.findById(orderId);
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelOrder", orderId.toString()));
        }
        var order = existing.get();
        transition.accept(order);
        return Result.success(fuelOrderRepository.save(order));
    }
}
