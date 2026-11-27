package com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CreateFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderCancelledEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.exceptions.IllegalOrderStateTransitionException;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderConfirmedEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderDispatchedEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class FuelOrder extends AbstractDomainAggregateRoot<FuelOrder> {

    /** Statuses an order can still be cancelled from; every other status is final or already in motion. */
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);

    private Long id;
    private Long requestId;
    private Long companyId;
    private Long providerId;
    private Long fuelProductId;
    private Long equipmentId;
    private Double requestedQuantity;
    private Double totalPrice;
    private OrderStatus status;
    private String deliveryAddress;
    private LocalDate scheduledDate;

    public FuelOrder(CreateFuelOrderCommand command, Double totalPrice) {
        this.companyId = command.companyId();
        this.providerId = command.providerId();
        this.fuelProductId = command.fuelProductId();
        this.equipmentId = command.equipmentId();
        this.requestedQuantity = command.requestedQuantity();
        this.totalPrice = totalPrice;
        this.status = OrderStatus.PENDING;
        this.deliveryAddress = command.deliveryAddress();
        this.scheduledDate = command.scheduledDate();
    }

    public void confirm() {
        requireStatus(OrderStatus.CONFIRMED, OrderStatus.PENDING);
        this.status = OrderStatus.CONFIRMED;
        registerDomainEvent(new FuelOrderConfirmedEvent(this.id, this.companyId));
    }

    public boolean canCancel() {
        return CANCELLABLE_STATUSES.contains(status);
    }

    public void cancel() {
        if (!canCancel()) throw new IllegalOrderStateTransitionException(status, OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        registerDomainEvent(new FuelOrderCancelledEvent(this.id, this.companyId));
    }

    public void dispatch() {
        if (status != OrderStatus.PENDING) throw new IllegalStateException("Order is not pending assignment");
        this.status = OrderStatus.DISPATCHED;
        registerDomainEvent(new FuelOrderDispatchedEvent(this.id, this.companyId));
    }

    /**
     * Saga compensation (T10.05): Fulfillment returns a DISPATCHED order to PENDING when
     * its own transaction fails after Ordering already applied the dispatch.
     */
    public void revertDispatch() {
        requireStatus(OrderStatus.PENDING, OrderStatus.DISPATCHED);
        this.status = OrderStatus.PENDING;
    }

    public void receive() {
        requireStatus(OrderStatus.PENDING_PAYMENT, OrderStatus.DISPATCHED);
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    public void markPaid() {
        requireStatus(OrderStatus.PAID, OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.PAID;
    }

    private void requireStatus(OrderStatus target, OrderStatus requiredCurrent) {
        if (status != requiredCurrent) throw new IllegalOrderStateTransitionException(status, target);
    }
}
