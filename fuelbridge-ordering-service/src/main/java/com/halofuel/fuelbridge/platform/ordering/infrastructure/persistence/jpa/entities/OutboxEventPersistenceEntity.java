package com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities;

import com.halofuel.fuelbridge.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 4 (T10.04b): row of the transactional outbox. It is written in the same
 * transaction as the fuel order change that raised the event, so an order transition is
 * never committed without its event, and the relay delivers it afterwards with retries.
 */
@Entity
@Table(name = "outbox_events", indexes = @Index(name = "ix_outbox_events_status_next_attempt",
        columnList = "status, nextAttemptAt"))
@Getter
@Setter
@NoArgsConstructor
public class OutboxEventPersistenceEntity extends AuditableAbstractPersistenceEntity {

    /** Unique identifier sent with the event so consumers can discard duplicates. */
    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false, length = 40)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    private Instant publishedAt;

    @Column(length = 500)
    private String lastError;

    public static OutboxEventPersistenceEntity pendingFuelOrderEvent(String eventType, Long orderId,
                                                                     Long companyId, Instant occurredAt) {
        var event = new OutboxEventPersistenceEntity();
        event.eventId = UUID.randomUUID().toString();
        event.eventType = eventType;
        event.aggregateType = "FuelOrder";
        event.aggregateId = orderId;
        event.companyId = companyId;
        event.occurredAt = occurredAt;
        event.status = OutboxEventStatus.PENDING;
        event.attempts = 0;
        event.nextAttemptAt = occurredAt;
        return event;
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    /**
     * Registers a failed delivery: the event stays PENDING until {@code nextAttemptAt}, or
     * becomes FAILED once it reaches {@code maxAttempts}.
     */
    public void registerFailedAttempt(String error, Instant nextAttemptAt, int maxAttempts) {
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
        this.nextAttemptAt = nextAttemptAt;
        if (this.attempts >= maxAttempts) {
            this.status = OutboxEventStatus.FAILED;
        }
    }
}
