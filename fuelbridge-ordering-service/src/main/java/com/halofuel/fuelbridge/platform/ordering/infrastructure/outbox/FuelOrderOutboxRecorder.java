package com.halofuel.fuelbridge.platform.ordering.infrastructure.outbox;

import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderCancelledEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderConfirmedEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderDispatchedEvent;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.OutboxEventPersistenceEntity;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.OutboxEventPersistenceRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Sprint 4 (T10.04b): stores every fuel order domain event in the outbox. The repository
 * publishes the events while it saves the order, and MANDATORY propagation makes this
 * write join that transaction: the order change and its event commit or roll back together.
 */
@Component
public class FuelOrderOutboxRecorder {

    public static final String ORDER_CONFIRMED = "FuelOrderConfirmed";
    public static final String ORDER_CANCELLED = "FuelOrderCancelled";
    public static final String ORDER_DISPATCHED = "FuelOrderDispatched";

    private final OutboxEventPersistenceRepository outbox;
    private final Clock clock;

    public FuelOrderOutboxRecorder(OutboxEventPersistenceRepository outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(FuelOrderConfirmedEvent event) {
        record(ORDER_CONFIRMED, event.orderId(), event.companyId());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(FuelOrderCancelledEvent event) {
        record(ORDER_CANCELLED, event.orderId(), event.companyId());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(FuelOrderDispatchedEvent event) {
        record(ORDER_DISPATCHED, event.orderId(), event.companyId());
    }

    private void record(String eventType, Long orderId, Long companyId) {
        outbox.save(OutboxEventPersistenceEntity.pendingFuelOrderEvent(eventType, orderId, companyId,
                clock.instant()));
    }
}
