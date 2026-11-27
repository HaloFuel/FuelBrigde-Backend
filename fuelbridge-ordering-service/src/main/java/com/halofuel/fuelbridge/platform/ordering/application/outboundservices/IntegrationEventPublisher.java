package com.halofuel.fuelbridge.platform.ordering.application.outboundservices;

import java.time.Instant;

/**
 * Outbound port through which the outbox relay delivers the Ordering integration events.
 * Today the adapter notifies the buyer through the Notification service over HTTP; a
 * message broker adapter can replace it without touching the outbox or the domain.
 */
public interface IntegrationEventPublisher {

    /**
     * Delivers the event. Delivery is at least once: the same event can arrive more than
     * once, so consumers should discard repeated {@code eventId} values.
     *
     * @throws RuntimeException when the event could not be delivered; the relay retries it
     */
    void publish(OrderIntegrationEvent event);

    record OrderIntegrationEvent(String eventId, String eventType, Long orderId, Long companyId,
                                 Instant occurredAt) {
    }
}
