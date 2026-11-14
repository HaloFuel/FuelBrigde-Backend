package com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CreateDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.valueobjects.DeliveryStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryTest {

    private static Delivery scheduledDelivery() {
        return new Delivery(new CreateDeliveryCommand(4101L, 202L, 801L, 701L, "2026-11-20", "Portón 3"));
    }

    @Test
    void aNewDeliveryIsScheduledWithTheCommandData() {
        var delivery = scheduledDelivery();

        assertEquals(DeliveryStatus.SCHEDULED, delivery.getStatus());
        assertEquals(4101L, delivery.getOrderId());
        assertEquals(801L, delivery.getDriverId());
        assertEquals(701L, delivery.getVehicleId());
        assertNull(delivery.getDispatchedAt());
    }

    @Test
    void dispatchingRecordsTheDispatchTime() {
        var delivery = scheduledDelivery();

        delivery.dispatch();

        assertEquals(DeliveryStatus.DISPATCHED, delivery.getStatus());
        assertNotNull(delivery.getDispatchedAt());
    }

    @Test
    void completingRecordsTheDeliveryTime() {
        var delivery = scheduledDelivery();
        delivery.dispatch();

        delivery.complete();

        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
        assertNotNull(delivery.getDeliveredAt());
    }

    @Test
    void failingKeepsTheReason() {
        var delivery = scheduledDelivery();
        delivery.dispatch();

        delivery.fail("Vía bloqueada en la Panamericana Sur");

        assertEquals(DeliveryStatus.FAILED, delivery.getStatus());
        assertEquals("Vía bloqueada en la Panamericana Sur", delivery.getNotes());
    }
}
