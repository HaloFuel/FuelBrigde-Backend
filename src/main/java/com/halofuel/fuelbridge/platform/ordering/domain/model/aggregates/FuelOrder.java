package com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CreateFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderCancelledEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderConfirmedEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderDispatchedEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class FuelOrder extends AbstractDomainAggregateRoot<FuelOrder> {

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
        this.status = OrderStatus.CONFIRMED;
        registerDomainEvent(new FuelOrderConfirmedEvent(this.id, this.companyId));
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        registerDomainEvent(new FuelOrderCancelledEvent(this.id, this.companyId));
    }

    public void dispatch() {
        if (status != OrderStatus.PENDING) throw new IllegalStateException("Order is not pending assignment");
        this.status = OrderStatus.DISPATCHED;
        registerDomainEvent(new FuelOrderDispatchedEvent(this.id, this.companyId));
    }

    public void receive() {
        if (status != OrderStatus.DISPATCHED) throw new IllegalStateException("Order is not dispatched");
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    public void markPaid() {
        if (status == OrderStatus.CANCELLED) throw new IllegalStateException("Cancelled orders cannot be paid");
        this.status = OrderStatus.PAID;
    }
}
