package com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderDispatchedEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FuelOrderDomainEventsTest {   

    /**
     * Sprint 1 - US-30
     * Verifies that dispatching an order registers
     * a notification event for the buyer company.
     */
    @Test
    void dispatchRegistersAnEventForTheBuyerCompany() {

        // Create a pending fuel order
        var order = new FuelOrder();
        order.setId(15L);
        order.setCompanyId(25L);
        order.setStatus(OrderStatus.PENDING);

        // Dispatch the fuel order
        order.dispatch();

        // Verify that the order status changes
        assertEquals(OrderStatus.DISPATCHED, order.getStatus());

        // Verify that the dispatch event is registered
        assertTrue(order.domainEvents().contains(
                new FuelOrderDispatchedEvent(15L, 25L)
        ));

        // Verify that only one event was registered
        assertEquals(1, order.domainEvents().size());
    }
}
