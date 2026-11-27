package com.halofuel.fuelbridge.platform.ordering.infrastructure.outbox;

import com.halofuel.fuelbridge.platform.FuelBridgeOrderingServiceApplication;
import com.halofuel.fuelbridge.platform.ordering.application.commandservices.FuelOrderCommandService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.DispatchFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.OutboxEventPersistenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * T10.04b: the order change and its outbox row share one transaction. When the outbox
 * row cannot be written, the order transition is rolled back too, so an order never
 * changes state without the event that announces it.
 */
@SpringBootTest(classes = FuelBridgeOrderingServiceApplication.class)
@ActiveProfiles("test")
class FuelOrderOutboxAtomicityTest {

    @Autowired
    private FuelOrderCommandService commands;

    @Autowired
    private FuelOrderRepository orders;

    @MockitoBean
    private OutboxEventPersistenceRepository outbox;

    @Test
    void theOrderTransitionIsRolledBackWhenTheOutboxWriteFails() {
        var order = new FuelOrder();
        order.setCompanyId(101L);
        order.setProviderId(202L);
        order.setFuelProductId(303L);
        order.setRequestedQuantity(500.0);
        order.setTotalPrice(2400.0);
        order.setDeliveryAddress("Av. Separadora Industrial 2450, Villa El Salvador, Lima");
        order.setScheduledDate(LocalDate.now().plusDays(1));
        order.setStatus(OrderStatus.PENDING);
        var stored = orders.save(order);
        when(outbox.save(any())).thenThrow(new RuntimeException("outbox table unavailable"));

        assertThrows(RuntimeException.class, () -> commands.handle(new DispatchFuelOrderCommand(stored.getId())));

        assertEquals(OrderStatus.PENDING, orders.findById(stored.getId()).orElseThrow().getStatus());
    }
}
