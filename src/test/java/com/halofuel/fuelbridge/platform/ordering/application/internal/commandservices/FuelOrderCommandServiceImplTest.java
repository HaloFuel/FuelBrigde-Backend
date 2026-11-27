package com.halofuel.fuelbridge.platform.ordering.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.FuelProductCatalog;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CancelFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.ConfirmFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.DispatchFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.RevertFuelOrderDispatchCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;

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
        service = new FuelOrderCommandServiceImpl(
                repository,
                mock(FuelProductCatalog.class)
        );
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

        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertSame(confirmed, service.handle(
                new ConfirmFuelOrderCommand(1L)
        ).toOptional().orElseThrow());

        assertSame(cancelled, service.handle(
                new CancelFuelOrderCommand(2L)
        ).toOptional().orElseThrow());

        assertSame(dispatched, service.handle(
                new DispatchFuelOrderCommand(3L)
        ).toOptional().orElseThrow());

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

        var result = service.handle(new DispatchFuelOrderCommand(1L));

        var failure = assertInstanceOf(Result.Failure.class, result);
        assertEquals("FUELORDER_CONFLICT", ((ApplicationError) failure.error()).code());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(repository, never()).save(any());
    }

    /**
     * Sprint 1 - US-12
     * Verifies that paid orders cannot be dispatched again.
     */
    @Test
    void paidOrdersCannotBeDispatched() {
        // Create an existing paid order
        var order = pendingOrder(4L);
        order.setStatus(OrderStatus.PAID);

        // Attempt to dispatch the paid order: rejected as a conflict
        assertTrue(service.handle(new DispatchFuelOrderCommand(4L)).isFailure());

        // Verify the original status is preserved
        assertEquals(OrderStatus.PAID, order.getStatus());

        // Verify no new event is registered
        assertTrue(order.domainEvents().isEmpty());

        // Verify the invalid change is not saved
        verify(repository, never()).save(any());
    }

    /**
     * Sprint 4 - T10.05: Saga compensation requested by Fulfillment.
     */
    @Test
    void revertDispatchReturnsADispatchedOrderToPending() {
        var order = pendingOrder(5L);
        order.setStatus(OrderStatus.DISPATCHED);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(new RevertFuelOrderDispatchCommand(5L));

        assertSame(order, result.toOptional().orElseThrow());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(repository).save(order);
    }

    @Test
    void revertDispatchOfAnOrderThatIsNotDispatchedIsAConflict() {
        var order = pendingOrder(6L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        var result = service.handle(new RevertFuelOrderDispatchCommand(6L));

        var failure = assertInstanceOf(Result.Failure.class, result);
        assertEquals("FUELORDER_CONFLICT", ((ApplicationError) failure.error()).code());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        verify(repository, never()).save(any());
    }

    private FuelOrder pendingOrder(Long id) {
        var order = new FuelOrder();
        order.setId(id);
        order.setCompanyId(10L);
        order.setStatus(OrderStatus.PENDING);

        when(repository.findById(id))
                .thenReturn(Optional.of(order));

        return order;
    }
}
