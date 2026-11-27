package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.infrastructure.outbox.OutboxRelay;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.OutboxEventPersistenceEntity;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.OutboxEventStatus;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.OutboxEventPersistenceRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Steps for fuel-order-outbox.feature (T10.04b): order events are stored in the outbox in
 * the same transaction as the order and delivered later by the relay, with retries.
 * The scheduled relay is disabled in tests, so each scenario runs it explicitly.
 */
public class OutboxSteps {

    @Autowired
    private OutboxEventPersistenceRepository outbox;

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private NotificationHttpStub notificationService;

    @Given("the notification service is unavailable")
    public void theNotificationServiceIsUnavailable() {
        notificationService.setAvailable(false);
    }

    @Given("the notification service is available again")
    public void theNotificationServiceIsAvailableAgain() {
        notificationService.setAvailable(true);
    }

    @When("the outbox relay runs")
    public void theOutboxRelayRuns() {
        relay.relayPending();
    }

    @Then("the outbox has {int} {string} event(s)")
    public void theOutboxHasEvents(int count, String status) {
        assertEquals(count, outbox.findByStatus(OutboxEventStatus.valueOf(status)).size());
    }

    @Then("the outbox holds a pending {string} event for the order")
    public void theOutboxHoldsAPendingEvent(String eventType) {
        var event = onlyEvent();
        assertEquals(eventType, event.getEventType());
        assertEquals("FuelOrder", event.getAggregateType());
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttempts());
        assertNotNull(event.getEventId());
    }

    @Then("the outbox event is {string} after {int} failed attempt(s)")
    public void theOutboxEventIsAfterFailedAttempts(String status, int attempts) {
        var event = onlyEvent();
        assertEquals(OutboxEventStatus.valueOf(status), event.getStatus());
        assertEquals(attempts, event.getAttempts());
        assertNotNull(event.getLastError());
    }

    @Then("the outbox event is published")
    public void theOutboxEventIsPublished() {
        var event = onlyEvent();
        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertNull(event.getLastError());
    }

    @Then("the notification carries the outbox event identifier")
    public void theNotificationCarriesTheOutboxEventIdentifier() {
        var eventId = onlyEvent().getEventId();
        assertTrue(notificationService.accepted().stream()
                .anyMatch(notification -> eventId.equals(notification.idempotencyKey())));
    }

    @Then("the outbox is empty")
    public void theOutboxIsEmpty() {
        assertTrue(outbox.findAll().isEmpty());
    }

    private OutboxEventPersistenceEntity onlyEvent() {
        var events = outbox.findAll();
        assertEquals(1, events.size(), "Expected exactly one outbox event");
        return events.getFirst();
    }
}
