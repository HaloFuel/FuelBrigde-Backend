package com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CreateDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.valueobjects.DeliveryStatus;
import com.halofuel.fuelbridge.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Delivery extends AbstractDomainAggregateRoot<Delivery> {

    private Long id;
    private Long orderId;
    private Long providerId;
    private Long driverId;
    private Long vehicleId;
    private DeliveryStatus status;
    private String scheduledDate;
    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
    private String notes;

    public Delivery(CreateDeliveryCommand command) {
        this.orderId = command.orderId();
        this.providerId = command.providerId();
        this.driverId = command.driverId();
        this.vehicleId = command.vehicleId();
        this.scheduledDate = command.scheduledDate();
        this.notes = command.notes();
        this.status = DeliveryStatus.SCHEDULED;
    }

    public void dispatch() {
        this.status = DeliveryStatus.DISPATCHED;
        this.dispatchedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = DeliveryStatus.FAILED;
        this.notes = reason;
    }
}
