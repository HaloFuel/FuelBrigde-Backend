package com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CreateFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @summary Pruebas unitarias del agregado FuelOrder.
 * @remarks Verifica el estado inicial del pedido y las transiciones de estado de
 * confirm(), cancel(), dispatch(), receive() y markPaid(), incluyendo los casos en los
 * que el agregado rechaza la operación con IllegalStateException, sin contexto de Spring ni mocks.
 * @author HaloFuel
 */
class FuelOrderTest {

    private FuelOrder newPendingOrder() {
        var command = new CreateFuelOrderCommand(
                10L, 20L, 30L, 40L, 500.0, "Av. Industrial 123, Lima", LocalDate.of(2026, 10, 15));
        return new FuelOrder(command, 7500.0);
    }

    @Test
    void newOrderStartsAsPendingAndCopiesCommandData() {
        // Arrange
        var command = new CreateFuelOrderCommand(
                10L, 20L, 30L, 40L, 500.0, "Av. Industrial 123, Lima", LocalDate.of(2026, 10, 15));

        // Act
        var order = new FuelOrder(command, 7500.0);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getId()).isNull();
        assertThat(order.getCompanyId()).isEqualTo(10L);
        assertThat(order.getProviderId()).isEqualTo(20L);
        assertThat(order.getFuelProductId()).isEqualTo(30L);
        assertThat(order.getEquipmentId()).isEqualTo(40L);
        assertThat(order.getRequestedQuantity()).isEqualTo(500.0);
        assertThat(order.getTotalPrice()).isEqualTo(7500.0);
        assertThat(order.getDeliveryAddress()).isEqualTo("Av. Industrial 123, Lima");
        assertThat(order.getScheduledDate()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void confirmMovesTheOrderToConfirmed() {
        // Arrange
        var order = newPendingOrder();

        // Act
        order.confirm();

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelMovesTheOrderToCancelled() {
        // Arrange
        var order = newPendingOrder();

        // Act
        order.cancel();

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void dispatchMovesAPendingOrderToDispatched() {
        // Arrange
        var order = newPendingOrder();

        // Act
        order.dispatch();

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DISPATCHED);
    }

    @Test
    void dispatchFailsWhenTheOrderIsNotPending() {
        // Arrange
        var order = newPendingOrder();
        order.confirm();

        // Act + Assert
        assertThatThrownBy(order::dispatch)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order is not pending assignment");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void receiveMovesADispatchedOrderToPendingPayment() {
        // Arrange
        var order = newPendingOrder();
        order.dispatch();

        // Act
        order.receive();

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void receiveFailsWhenTheOrderWasNeverDispatched() {
        // Arrange
        var order = newPendingOrder();

        // Act + Assert
        assertThatThrownBy(order::receive)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order is not dispatched");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void markPaidMovesANonCancelledOrderToPaid() {
        // Arrange
        var order = newPendingOrder();
        order.dispatch();
        order.receive();

        // Act
        order.markPaid();

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void markPaidFailsWhenTheOrderIsCancelled() {
        // Arrange
        var order = newPendingOrder();
        order.cancel();

        // Act + Assert
        assertThatThrownBy(order::markPaid)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cancelled orders cannot be paid");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
