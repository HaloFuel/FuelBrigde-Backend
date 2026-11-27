package com.halofuel.fuelbridge.platform.ordering.infrastructure.outbox;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.IntegrationEventPublisher;
import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.IntegrationEventPublisher.OrderIntegrationEvent;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.OutboxEventPersistenceEntity;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.OutboxEventStatus;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.OutboxEventPersistenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Sprint 4 (T10.04b): delivers the pending outbox events through the
 * {@link IntegrationEventPublisher} port. It runs outside any database transaction, so a
 * slow remote call never holds a lock on the orders. A failed delivery is retried with
 * exponential backoff (base, 2x base, 4x base... up to five minutes) and the event becomes
 * FAILED after the maximum number of attempts.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    private final OutboxEventPersistenceRepository outbox;
    private final IntegrationEventPublisher publisher;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration retryBaseDelay;

    public OutboxRelay(OutboxEventPersistenceRepository outbox, IntegrationEventPublisher publisher, Clock clock,
                       @Value("${ordering.outbox.max-attempts:10}") int maxAttempts,
                       @Value("${ordering.outbox.retry-base-delay-ms:2000}") long retryBaseDelayMs) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.retryBaseDelay = Duration.ofMillis(retryBaseDelayMs);
    }

    /** @return how many events were delivered in this run */
    public int relayPending() {
        var batch = outbox.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
                OutboxEventStatus.PENDING, clock.instant());
        int delivered = 0;
        for (var event : batch) {
            try {
                publisher.publish(toIntegrationEvent(event));
                event.markPublished(clock.instant());
                delivered++;
            } catch (RuntimeException ex) {
                event.registerFailedAttempt(ex.getMessage(), clock.instant().plus(backoffAfter(event.getAttempts() + 1)),
                        maxAttempts);
                if (event.getStatus() == OutboxEventStatus.FAILED) {
                    log.error("Outbox event {} ({} of order {}) failed after {} attempts: {}", event.getEventId(),
                            event.getEventType(), event.getAggregateId(), event.getAttempts(), ex.getMessage());
                } else {
                    log.warn("Outbox event {} ({} of order {}) will be retried: {}", event.getEventId(),
                            event.getEventType(), event.getAggregateId(), ex.getMessage());
                }
            }
            outbox.save(event);
        }
        return delivered;
    }

    /** Delay before the next attempt once {@code attempt} deliveries have failed. */
    Duration backoffAfter(int attempt) {
        long factor = 1L << Math.min(attempt - 1, 20);
        var delay = retryBaseDelay.multipliedBy(factor);
        return delay.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : delay;
    }

    private static OrderIntegrationEvent toIntegrationEvent(OutboxEventPersistenceEntity event) {
        return new OrderIntegrationEvent(event.getEventId(), event.getEventType(), event.getAggregateId(),
                event.getCompanyId(), event.getOccurredAt());
    }
}
