package com.halofuel.fuelbridge.platform.fulfillment.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.EquipmentFuelReceipts;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.EquipmentFuelReceipts.FuelReceiptOutcome;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.FuelOrders;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.FuelOrders.FuelOrderSnapshot;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.FuelOrders.OrderingUnavailableException;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.FuelOrders.TransitionOutcome;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.InventoryStock;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.InventoryStock.StockReservationOutcome;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Delivery;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Driver;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Vehicle;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CompleteDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CreateDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.valueobjects.DeliveryStatus;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DeliveryRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DriverRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.VehicleRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Fulfillment as a standalone service: scheduling a delivery reserves stock in Inventory and
 * requests the dispatch to Ordering (the reservation is released if Ordering rejects it or the
 * local changes fail); completing it registers the fuel receipt in Equipment and requests the
 * receive transition to Ordering. Nothing is saved when a remote service rejects the operation.
 */
class DeliveryCommandServiceImplTest {

    private static final long DELIVERY_ID = 1L;
    private static final long ORDER_ID = 10L;

    private DeliveryRepository deliveries;
    private DriverRepository drivers;
    private VehicleRepository vehicles;
    private FuelOrders orders;
    private EquipmentFuelReceipts equipmentFuelReceipts;
    private InventoryStock inventoryStock;
    private DeliveryCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        deliveries = mock(DeliveryRepository.class);
        drivers = mock(DriverRepository.class);
        vehicles = mock(VehicleRepository.class);
        orders = mock(FuelOrders.class);
        equipmentFuelReceipts = mock(EquipmentFuelReceipts.class);
        inventoryStock = mock(InventoryStock.class);
        service = new DeliveryCommandServiceImpl(deliveries, drivers, vehicles, orders,
                inventoryStock, equipmentFuelReceipts);
        when(deliveries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveries.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
    }

    private static String errorCode(Result<Delivery, ApplicationError> result) {
        return assertInstanceOf(Result.Failure.class, result).error() instanceof ApplicationError error
                ? error.code() : null;
    }

    // ------------------------------------------------------------------ completion

    private void orderInOrdering(Long equipmentId, String status) {
        when(orders.findById(ORDER_ID)).thenReturn(Optional.of(
                new FuelOrderSnapshot(ORDER_ID, 202L, 7L, equipmentId, 240.0, status)));
    }

    private Delivery dispatchedDelivery() {
        var delivery = new Delivery(new CreateDeliveryCommand(ORDER_ID, 202L, 3L, 4L, "2026-10-15", null));
        delivery.dispatch();
        when(deliveries.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));
        return delivery;
    }

    @Test
    void completingADeliveryRegistersTheFuelReceiptAndRequestsTheReceive() {
        orderInOrdering(305L, "DISPATCHED");
        var delivery = dispatchedDelivery();
        when(equipmentFuelReceipts.register(305L, 240.0)).thenReturn(FuelReceiptOutcome.REGISTERED);
        when(orders.receive(ORDER_ID)).thenReturn(TransitionOutcome.APPLIED);

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
        verify(equipmentFuelReceipts).register(305L, 240.0);
        verify(orders).receive(ORDER_ID);
        verify(deliveries).save(delivery);
    }

    @Test
    void nothingIsSavedWhenTheEquipmentServiceIsUnavailable() {
        orderInOrdering(305L, "DISPATCHED");
        var delivery = dispatchedDelivery();
        when(equipmentFuelReceipts.register(305L, 240.0)).thenReturn(FuelReceiptOutcome.UNAVAILABLE);

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertEquals("UNEXPECTED_ERROR", errorCode(result));
        assertEquals(DeliveryStatus.DISPATCHED, delivery.getStatus());
        verify(orders, never()).receive(any());
        verify(deliveries, never()).save(any());
        verify(drivers, never()).save(any());
        verify(vehicles, never()).save(any());
    }

    @Test
    void deliveryIsCompletedWhenTheEquipmentNoLongerExists() {
        orderInOrdering(305L, "DISPATCHED");
        var delivery = dispatchedDelivery();
        when(equipmentFuelReceipts.register(305L, 240.0)).thenReturn(FuelReceiptOutcome.EQUIPMENT_NOT_FOUND);
        when(orders.receive(ORDER_ID)).thenReturn(TransitionOutcome.APPLIED);

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
    }

    @Test
    void ordersWithoutEquipmentDoNotCallTheEquipmentService() {
        orderInOrdering(null, "DISPATCHED");
        dispatchedDelivery();
        when(orders.receive(ORDER_ID)).thenReturn(TransitionOutcome.APPLIED);

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertTrue(result.isSuccess());
        verifyNoInteractions(equipmentFuelReceipts);
    }

    @Test
    void anOrderThatIsNotDispatchedCannotBeCompleted() {
        orderInOrdering(305L, "PENDING");
        var delivery = dispatchedDelivery();

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertEquals("DELIVERY_CONFLICT", errorCode(result));
        assertEquals(DeliveryStatus.DISPATCHED, delivery.getStatus());
        verifyNoInteractions(equipmentFuelReceipts);
        verify(orders, never()).receive(any());
    }

    @Test
    void nothingIsSavedWhenOrderingRejectsTheReceive() {
        orderInOrdering(null, "DISPATCHED");
        var delivery = dispatchedDelivery();
        when(orders.receive(ORDER_ID)).thenReturn(TransitionOutcome.REJECTED);

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertEquals("DELIVERY_CONFLICT", errorCode(result));
        assertEquals(DeliveryStatus.DISPATCHED, delivery.getStatus());
        verify(deliveries, never()).save(any());
    }

    // ------------------------------------------------------------------ scheduling

    private CreateDeliveryCommand scheduleRequest(String orderStatus) {
        var driver = new Driver(202L, "Juan", "Perez", "Q12345678", "999111222", "juan@example.com", "AVAILABLE");
        driver.setId(3L);
        var vehicle = new Vehicle(202L, "ABC-123", "Volvo", "FH", 500.0, "GAL", "AVAILABLE");
        vehicle.setId(4L);
        when(drivers.findById(3L)).thenReturn(Optional.of(driver));
        when(vehicles.findById(4L)).thenReturn(Optional.of(vehicle));
        orderInOrdering(null, orderStatus);
        return new CreateDeliveryCommand(ORDER_ID, 202L, 3L, 4L, "2026-10-15", null);
    }

    @Test
    void schedulingADeliveryReservesTheStockAndRequestsTheDispatch() {
        var command = scheduleRequest("PENDING");
        when(inventoryStock.reserve(7L, 240.0)).thenReturn(StockReservationOutcome.RESERVED);
        when(orders.dispatch(ORDER_ID)).thenReturn(TransitionOutcome.APPLIED);

        var result = service.handle(command);

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.DISPATCHED, result.toOptional().orElseThrow().getStatus());
        verify(inventoryStock).reserve(7L, 240.0);
        verify(orders).dispatch(ORDER_ID);
        verify(inventoryStock, never()).release(any(), any());
    }

    @Test
    void insufficientStockRejectsTheDeliveryWithoutSavingOrDispatching() {
        var command = scheduleRequest("PENDING");
        when(inventoryStock.reserve(7L, 240.0)).thenReturn(StockReservationOutcome.INSUFFICIENT_STOCK);

        var result = service.handle(command);

        assertEquals("DELIVERY_CONFLICT", errorCode(result));
        verify(drivers, never()).save(any());
        verify(vehicles, never()).save(any());
        verify(deliveries, never()).save(any());
        verify(orders, never()).dispatch(any());
    }

    @Test
    void reservedStockIsReleasedWhenTheDeliveryCannotBeSaved() {
        var command = scheduleRequest("PENDING");
        when(inventoryStock.reserve(7L, 240.0)).thenReturn(StockReservationOutcome.RESERVED);
        when(deliveries.save(any())).thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> service.handle(command));

        verify(inventoryStock).release(7L, 240.0);
        verify(orders, never()).dispatch(any());
    }

    @Test
    void reservedStockIsReleasedWhenOrderingRejectsTheDispatch() {
        var command = scheduleRequest("PENDING");
        when(inventoryStock.reserve(7L, 240.0)).thenReturn(StockReservationOutcome.RESERVED);
        when(orders.dispatch(ORDER_ID)).thenReturn(TransitionOutcome.REJECTED);

        var result = service.handle(command);

        assertEquals("DELIVERY_CONFLICT", errorCode(result));
        verify(inventoryStock).release(7L, 240.0);
    }

    // ------------------------------------------------------------------ compensation (T10.05)

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /** Runs the command as if inside a transaction and finishes it with the given status. */
    private Result<Delivery, ApplicationError> scheduleInTransactionEndingWith(CreateDeliveryCommand command,
                                                                              int completionStatus) {
        TransactionSynchronizationManager.initSynchronization();
        var result = service.handle(command);
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        TransactionSynchronizationManager.clearSynchronization();
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(completionStatus));
        return result;
    }

    @Test
    void theDispatchIsRevertedAndTheStockReleasedWhenTheLocalCommitFails() {
        var command = scheduleRequest("PENDING");
        when(inventoryStock.reserve(7L, 240.0)).thenReturn(StockReservationOutcome.RESERVED);
        when(orders.dispatch(ORDER_ID)).thenReturn(TransitionOutcome.APPLIED);
        when(orders.revertDispatch(ORDER_ID)).thenReturn(TransitionOutcome.APPLIED);

        scheduleInTransactionEndingWith(command, TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(orders).revertDispatch(ORDER_ID);
        verify(inventoryStock).release(7L, 240.0);
    }

    @Test
    void nothingIsCompensatedWhenTheLocalCommitSucceeds() {
        var command = scheduleRequest("PENDING");
        when(inventoryStock.reserve(7L, 240.0)).thenReturn(StockReservationOutcome.RESERVED);
        when(orders.dispatch(ORDER_ID)).thenReturn(TransitionOutcome.APPLIED);

        var result = scheduleInTransactionEndingWith(command, TransactionSynchronization.STATUS_COMMITTED);

        assertTrue(result.isSuccess());
        verify(orders, never()).revertDispatch(any());
        verify(inventoryStock, never()).release(any(), any());
    }

    @Test
    void aRejectedDispatchIsNotReverted() {
        var command = scheduleRequest("PENDING");
        when(inventoryStock.reserve(7L, 240.0)).thenReturn(StockReservationOutcome.RESERVED);
        when(orders.dispatch(ORDER_ID)).thenReturn(TransitionOutcome.REJECTED);

        scheduleInTransactionEndingWith(command, TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(orders, never()).revertDispatch(any());
        verify(inventoryStock).release(7L, 240.0);
    }

    @Test
    void theStockIsStillReleasedWhenTheRevertCannotReachOrdering() {
        var command = scheduleRequest("PENDING");
        when(inventoryStock.reserve(7L, 240.0)).thenReturn(StockReservationOutcome.RESERVED);
        when(orders.dispatch(ORDER_ID)).thenReturn(TransitionOutcome.APPLIED);
        when(orders.revertDispatch(ORDER_ID)).thenReturn(TransitionOutcome.UNAVAILABLE);

        scheduleInTransactionEndingWith(command, TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(orders).revertDispatch(ORDER_ID);
        verify(inventoryStock).release(7L, 240.0);
    }

    @Test
    void anOrderThatIsNotPendingIsRejectedBeforeReservingStock() {
        var command = scheduleRequest("CANCELLED");

        var result = service.handle(command);

        assertEquals("DELIVERY_CONFLICT", errorCode(result));
        verifyNoInteractions(inventoryStock);
        verify(orders, never()).dispatch(any());
    }

    @Test
    void anUnreachableOrderingServiceRejectsSchedulingWithoutReservingStock() {
        var command = scheduleRequest("PENDING");
        when(orders.findById(ORDER_ID)).thenThrow(new OrderingUnavailableException("down", null));

        var result = service.handle(command);

        assertEquals("UNEXPECTED_ERROR", errorCode(result));
        verifyNoInteractions(inventoryStock);
    }

    @Test
    void anOrderOfAnotherProviderIsReportedAsNotFound() {
        var command = new CreateDeliveryCommand(ORDER_ID, 202L, 3L, 4L, "2026-10-15", null);
        scheduleRequest("PENDING");
        when(orders.findById(ORDER_ID)).thenReturn(Optional.of(
                new FuelOrderSnapshot(ORDER_ID, 999L, 7L, null, 240.0, "PENDING")));

        var result = service.handle(command);

        assertEquals("FUELORDER_NOT_FOUND", errorCode(result));
        verifyNoInteractions(inventoryStock);
    }
}
