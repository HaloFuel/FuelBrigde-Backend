package com.halofuel.fuelbridge.platform.notification.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Dedupe record for an integration event already handled off the "order-events" broker
 * (T10.04a). Delivery is at least once, so the same {@code eventId} can arrive more than
 * once; its presence here means the notification it caused was already created.
 */
@Entity
@Table(name = "processed_event_records")
@Getter
@NoArgsConstructor
public class ProcessedEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedEventRecord(String eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }
}
