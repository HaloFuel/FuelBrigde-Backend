package com.halofuel.fuelbridge.platform.ordering.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.inventory.application.queryservices.FuelProductQueryService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CancelFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.ConfirmFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.DispatchFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FuelOrderCommandServiceImplTest {
    private FuelOrderRepository repository;
    private FuelOrderCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(FuelOrderRepository.class);
        service = new FuelOrderCommandServiceImpl(repository, mock(FuelProductQueryService.class));
    }

    @Test
    void missingOrdersFailWithoutSavingForAllTransitions() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertTrue(service.handle(new ConfirmFuelOrderCommand(1L)).isFailure());
        assertTrue(service.handle(new CancelFuelOrderCommand(1L)).isFailure());
        assertTrue(service.handle(new DispatchFuelOrderCommand(1L)).isFailure());

        verify(repository, never()).save(any());
    }

    @Test
    void successfulTransitionsPersistTheNewStatus() {
        var confirmed = pendingOrder(1L);
        var cancelled = pendingOrder(2L);
        var dispatched = pendingOrder(3L);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertSame(confirmed, service.handle(new ConfirmFuelOrderCommand(1L)).toOptional().orElseThrow());
        assertSame(cancelled, service.handle(new CancelFuelOrderCommand(2L)).toOptional().orElseThrow());
        assertSame(dispatched, service.handle(new DispatchFuelOrderCommand(3L)).toOptional().orElseThrow());
        assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(OrderStatus.DISPATCHED, dispatched.getStatus());
        verify(repository).save(confirmed);
        verify(repository).save(cancelled);
        verify(repository).save(dispatched);
    }

    @Test
    void rejectedDispatchDoesNotSaveOrChangeStatus() {
        var order = pendingOrder(1L);
        order.setStatus(OrderStatus.CANCELLED);

        assertThrows(IllegalStateException.class, () -> service.handle(new DispatchFuelOrderCommand(1L)));

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(repository, never()).save(any());
    }

    private FuelOrder pendingOrder(Long id) {
        var order = new FuelOrder();
        order.setId(id);
        order.setCompanyId(10L);
        order.setStatus(OrderStatus.PENDING);
        when(repository.findById(id)).thenReturn(Optional.of(order));
        return order;
    }
}
