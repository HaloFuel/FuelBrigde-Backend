package com.halofuel.fuelbridge.platform.ordering.infrastructure.broker;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.IntegrationEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Broker-backed adapter of {@link IntegrationEventPublisher} (T10.04a): publishes each
 * Ordering event to the "order-events" topic exchange, with the event type as routing key,
 * so delivery survives Notification being down. Not registered as a Spring bean on purpose:
 * {@link com.halofuel.fuelbridge.platform.ordering.infrastructure.notification.NotificationServiceEventPublisher}
 * remains the only active {@link IntegrationEventPublisher} until the outbox is rewired to
 * this adapter in a later task.
 */
public class RabbitIntegrationEventPublisher implements IntegrationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitIntegrationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(OrderIntegrationEvent event) {
        rabbitTemplate.convertAndSend(OrderEventsRabbitConfiguration.EXCHANGE_NAME, event.eventType(), event);
    }
}
