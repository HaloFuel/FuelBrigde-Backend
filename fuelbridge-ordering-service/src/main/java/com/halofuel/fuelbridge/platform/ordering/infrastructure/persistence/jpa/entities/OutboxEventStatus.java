package com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities;

/** Delivery state of an event stored in the transactional outbox. */
public enum OutboxEventStatus {
    /** Waiting for the relay, either for the first time or for a retry. */
    PENDING,
    /** Delivered to the integration channel. */
    PUBLISHED,
    /** Discarded after the maximum number of attempts; needs manual review. */
    FAILED
}
