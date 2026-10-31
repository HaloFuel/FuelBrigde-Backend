package com.halofuel.fuelbridge.platform.fulfillment.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.EquipmentFuelReceipts;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.EquipmentFuelReceipts.FuelReceiptOutcome;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Delivery;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CompleteDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CreateDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.valueobjects.DeliveryStatus;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DeliveryRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DriverRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.VehicleRepository;
import com.halofuel.fuelbridge.platform.inventory.domain.repositories.FuelProductRepository;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CreateFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Completion of a delivery after Equipment was extracted: Fulfillment registers the fuel
 * receipt through the outbound port and saves nothing when the Equipment service is down.
 */
class DeliveryCommandServiceImplTest {

    private static final long DELIVERY_ID = 1L;
    private static final long ORDER_ID = 10L;

    private DeliveryRepository deliveries;
    private DriverRepository drivers;
    private VehicleRepository vehicles;
    private FuelOrderRepository orders;
    private EquipmentFuelReceipts equipmentFuelReceipts;
    private DeliveryCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        deliveries = mock(DeliveryRepository.class);
        drivers = mock(DriverRepository.class);
        vehicles = mock(VehicleRepository.class);
        orders = mock(FuelOrderRepository.class);
        equipmentFuelReceipts = mock(EquipmentFuelReceipts.class);
        service = new DeliveryCommandServiceImpl(deliveries, drivers, vehicles, orders,
                mock(FuelProductRepository.class), equipmentFuelReceipts);
        when(deliveries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private FuelOrder dispatchedOrder(Long equipmentId) {
        var order = new FuelOrder(new CreateFuelOrderCommand(101L, 202L, 7L, equipmentId, 240.0,
                "Av. Industrial 123, Lima", LocalDate.of(2026, 10, 15)), 7500.0);
        order.dispatch();
        when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));
        return order;
    }

    private Delivery dispatchedDelivery() {
        var delivery = new Delivery(new CreateDeliveryCommand(ORDER_ID, 202L, 3L, 4L, "2026-10-15", null));
        delivery.dispatch();
        when(deliveries.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));
        return delivery;
    }

    @Test
    void completingADeliveryRegistersTheFuelReceiptForTheEquipment() {
        var order = dispatchedOrder(305L);
        var delivery = dispatchedDelivery();
        when(equipmentFuelReceipts.register(305L, 240.0)).thenReturn(FuelReceiptOutcome.REGISTERED);

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        verify(equipmentFuelReceipts).register(305L, 240.0);
        verify(orders).save(order);
        verify(deliveries).save(delivery);
    }

    @Test
    void nothingIsSavedWhenTheEquipmentServiceIsUnavailable() {
        dispatchedOrder(305L);
        var delivery = dispatchedDelivery();
        when(equipmentFuelReceipts.register(305L, 240.0)).thenReturn(FuelReceiptOutcome.UNAVAILABLE);

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertTrue(result.isFailure());
        assertEquals(DeliveryStatus.DISPATCHED, delivery.getStatus());
        verify(orders, never()).save(any());
        verify(deliveries, never()).save(any());
        verify(drivers, never()).save(any());
        verify(vehicles, never()).save(any());
    }

    @Test
    void deliveryIsCompletedWhenTheEquipmentNoLongerExists() {
        dispatchedOrder(305L);
        var delivery = dispatchedDelivery();
        when(equipmentFuelReceipts.register(305L, 240.0)).thenReturn(FuelReceiptOutcome.EQUIPMENT_NOT_FOUND);

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
    }

    @Test
    void ordersWithoutEquipmentDoNotCallTheEquipmentService() {
        dispatchedOrder(null);
        dispatchedDelivery();

        var result = service.handle(new CompleteDeliveryCommand(DELIVERY_ID));

        assertTrue(result.isSuccess());
        verifyNoInteractions(equipmentFuelReceipts);
    }
}
