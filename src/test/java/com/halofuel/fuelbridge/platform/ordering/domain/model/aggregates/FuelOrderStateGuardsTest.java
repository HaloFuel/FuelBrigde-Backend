package com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CreateFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.exceptions.IllegalOrderStateTransitionException;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @summary Pruebas unitarias de las guardas de estado de FuelOrder.
 * @remarks Recorre todos los valores de OrderStatus para confirm(), cancel(), receive() y markPaid():
 * cada estado válido transiciona y emite su evento, y cada estado inválido lanza
 * IllegalOrderStateTransitionException sin cambiar el estado ni registrar eventos.
 * dispatch() queda fuera de este alcance.
 * @author HaloFuel
 */
class FuelOrderStateGuardsTest {

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);

    private FuelOrder orderIn(OrderStatus status) {
        var order = new FuelOrder(new CreateFuelOrderCommand(
                10L, 20L, 30L, 40L, 500.0, "Av. Industrial 123, Lima", LocalDate.of(2026, 10, 15)), 7500.0);
        order.setStatus(status);
        return order;
    }

    // ---- cancel ----

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED"})
    void cancelIsAllowedFromPendingAndConfirmed(OrderStatus from) {
        var order = orderIn(from);

        assertThat(order.canCancel()).isTrue();
        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.domainEvents()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"PENDING", "CONFIRMED"})
    void cancelIsRejectedFromEveryOtherStatus(OrderStatus from) {
        var order = orderIn(from);

        assertThat(order.canCancel()).isFalse();
        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalOrderStateTransitionException.class)
                .hasMessage("Cannot transition from %s to CANCELLED".formatted(from));

        assertThat(order.getStatus()).isEqualTo(from);
        assertThat(order.domainEvents()).isEmpty();
    }

    @Test
    void canCancelIsTrueExactlyForTheCancellableStatuses() {
        for (var status : OrderStatus.values()) {
            assertThat(orderIn(status).canCancel()).as(status.name()).isEqualTo(CANCELLABLE.contains(status));
        }
    }

    // ---- confirm ----

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "PENDING")
    void confirmIsAllowedOnlyFromPending(OrderStatus from) {
        var order = orderIn(from);

        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.domainEvents()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
    void confirmIsRejectedFromEveryOtherStatus(OrderStatus from) {
        var order = orderIn(from);

        assertThatThrownBy(order::confirm)
                .isInstanceOf(IllegalOrderStateTransitionException.class)
                .hasMessage("Cannot transition from %s to CONFIRMED".formatted(from));

        assertThat(order.getStatus()).isEqualTo(from);
        assertThat(order.domainEvents()).isEmpty();
    }

    // ---- receive ----

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "DISPATCHED")
    void receiveIsAllowedOnlyFromDispatched(OrderStatus from) {
        var order = orderIn(from);

        order.receive();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DISPATCHED")
    void receiveIsRejectedFromEveryOtherStatus(OrderStatus from) {
        var order = orderIn(from);

        assertThatThrownBy(order::receive)
                .isInstanceOf(IllegalOrderStateTransitionException.class)
                .hasMessage("Cannot transition from %s to PENDING_PAYMENT".formatted(from));

        assertThat(order.getStatus()).isEqualTo(from);
    }

    // ---- markPaid ----

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "PENDING_PAYMENT")
    void markPaidIsAllowedOnlyFromPendingPayment(OrderStatus from) {
        var order = orderIn(from);

        order.markPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING_PAYMENT")
    void markPaidIsRejectedFromEveryOtherStatus(OrderStatus from) {
        var order = orderIn(from);

        assertThatThrownBy(order::markPaid)
                .isInstanceOf(IllegalOrderStateTransitionException.class)
                .hasMessage("Cannot transition from %s to PAID".formatted(from));

        assertThat(order.getStatus()).isEqualTo(from);
    }

    // ---- exception ----

    @Test
    void theExceptionIsAnIllegalStateExceptionThatExposesBothStates() {
        var ex = new IllegalOrderStateTransitionException(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

        assertThat(ex).isInstanceOf(IllegalStateException.class);
        assertThat(ex.getCurrentState()).isEqualTo("DELIVERED");
        assertThat(ex.getTargetState()).isEqualTo("CANCELLED");
        assertThat(ex.getMessage()).isEqualTo("Cannot transition from DELIVERED to CANCELLED");
    }

    @Test
    void aFullHappyPathStillWorks() {
        var order = orderIn(OrderStatus.PENDING);

        order.dispatch();
        order.receive();
        order.markPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
