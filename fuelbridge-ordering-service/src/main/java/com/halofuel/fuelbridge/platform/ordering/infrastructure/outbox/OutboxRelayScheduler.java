package com.halofuel.fuelbridge.platform.ordering.infrastructure.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the outbox relay periodically. Tests disable it with
 * {@code ordering.outbox.relay.enabled=false} and run the relay explicitly.
 */
@Component
@ConditionalOnProperty(prefix = "ordering.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private final OutboxRelay relay;

    public OutboxRelayScheduler(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${ordering.outbox.relay.delay-ms:2000}")
    public void relayPendingEvents() {
        relay.relayPending();
    }
}
