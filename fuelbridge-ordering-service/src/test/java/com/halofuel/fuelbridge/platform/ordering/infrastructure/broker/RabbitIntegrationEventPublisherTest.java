package com.halofuel.fuelbridge.platform.ordering.infrastructure.broker;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.IntegrationEventPublisher.OrderIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RabbitIntegrationEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitIntegrationEventPublisher publisher = new RabbitIntegrationEventPublisher(rabbitTemplate);

    @Test
    void publishesTheEventToTheOrderEventsExchangeWithTheEventTypeAsRoutingKey() {
        var event = new OrderIntegrationEvent("evt-1", "FuelOrderConfirmed", 4101L, 101L, Instant.parse("2026-11-26T10:00:00Z"));

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(eq(OrderEventsRabbitConfiguration.EXCHANGE_NAME),
                eq("FuelOrderConfirmed"), eq(event));
    }

    @Test
    void letsABrokerFailurePropagateSoTheOutboxRelayRetries() {
        var event = new OrderIntegrationEvent("evt-2", "FuelOrderCancelled", 4102L, 102L, Instant.now());
        doThrow(new AmqpException("broker unreachable")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), eq(event));

        assertThatThrownBy(() -> publisher.publish(event)).isInstanceOf(AmqpException.class);
    }
}
