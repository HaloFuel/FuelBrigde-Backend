package com.halofuel.fuelbridge.platform.ordering.infrastructure.outbox;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.IntegrationEventPublisher;
import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.IntegrationEventPublisher.OrderIntegrationEvent;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.OutboxEventPersistenceEntity;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.OutboxEventStatus;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.OutboxEventPersistenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** T10.04b: delivery, retry with exponential backoff and discard of the outbox events. */
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-11-26T15:00:00Z");
    private static final int MAX_ATTEMPTS = 3;

    private OutboxEventPersistenceRepository outbox;
    private IntegrationEventPublisher publisher;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        outbox = mock(OutboxEventPersistenceRepository.class);
        publisher = mock(IntegrationEventPublisher.class);
        relay = new OutboxRelay(outbox, publisher, Clock.fixed(NOW, ZoneOffset.UTC), MAX_ATTEMPTS, 2000);
    }

    private OutboxEventPersistenceEntity pending(long orderId) {
        return OutboxEventPersistenceEntity.pendingFuelOrderEvent(FuelOrderOutboxRecorder.ORDER_DISPATCHED,
                orderId, 101L, NOW.minusSeconds(5));
    }

    private void batch(OutboxEventPersistenceEntity... events) {
        when(outbox.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(OutboxEventStatus.PENDING, NOW))
                .thenReturn(List.of(events));
    }

    @Test
    void aDeliveredEventIsMarkedAsPublished() {
        var event = pending(10L);
        batch(event);

        assertEquals(1, relay.relayPending());

        var published = ArgumentCaptor.forClass(OrderIntegrationEvent.class);
        verify(publisher).publish(published.capture());
        assertEquals(event.getEventId(), published.getValue().eventId());
        assertEquals("FuelOrderDispatched", published.getValue().eventType());
        assertEquals(10L, published.getValue().orderId());
        assertEquals(101L, published.getValue().companyId());
        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
        assertEquals(NOW, event.getPublishedAt());
        verify(outbox).save(event);
    }

    @Test
    void aFailedDeliveryStaysPendingUntilTheBackoffExpires() {
        var event = pending(10L);
        batch(event);
        doThrow(new IllegalStateException("Notification unavailable")).when(publisher).publish(any());

        assertEquals(0, relay.relayPending());

        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(1, event.getAttempts());
        assertEquals("Notification unavailable", event.getLastError());
        assertEquals(NOW.plusSeconds(2), event.getNextAttemptAt());
        verify(outbox).save(event);
    }

    @Test
    void anEventIsDiscardedAfterTheMaximumNumberOfAttempts() {
        var event = pending(10L);
        event.setAttempts(MAX_ATTEMPTS - 1);
        batch(event);
        doThrow(new IllegalStateException("Notification unavailable")).when(publisher).publish(any());

        relay.relayPending();

        assertEquals(OutboxEventStatus.FAILED, event.getStatus());
        assertEquals(MAX_ATTEMPTS, event.getAttempts());
    }

    @Test
    void oneFailedEventDoesNotStopTheRestOfTheBatch() {
        var failing = pending(10L);
        var delivered = pending(11L);
        batch(failing, delivered);
        doThrow(new IllegalStateException("boom")).when(publisher)
                .publish(argThatOrder(10L));

        assertEquals(1, relay.relayPending());

        assertEquals(OutboxEventStatus.PENDING, failing.getStatus());
        assertEquals(OutboxEventStatus.PUBLISHED, delivered.getStatus());
    }

    @Test
    void theBackoffDoublesUpToFiveMinutes() {
        assertEquals(Duration.ofSeconds(2), relay.backoffAfter(1));
        assertEquals(Duration.ofSeconds(4), relay.backoffAfter(2));
        assertEquals(Duration.ofSeconds(8), relay.backoffAfter(3));
        assertEquals(Duration.ofMinutes(5), relay.backoffAfter(30));
    }

    @Test
    void nothingIsPublishedWhenThereAreNoPendingEvents() {
        batch();

        assertEquals(0, relay.relayPending());

        verifyNoInteractions(publisher);
        verify(outbox, never()).save(any());
    }

    private static OrderIntegrationEvent argThatOrder(long orderId) {
        return org.mockito.ArgumentMatchers.argThat(event -> event != null && event.orderId() == orderId);
    }
}
