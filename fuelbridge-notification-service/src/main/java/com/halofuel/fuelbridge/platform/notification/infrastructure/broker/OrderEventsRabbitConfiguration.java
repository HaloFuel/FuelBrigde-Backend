package com.halofuel.fuelbridge.platform.notification.infrastructure.broker;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology of the "order-events" topic exchange: Ordering is the producer, Notification the
 * consumer. Both services declare the same exchange, queue and binding (T10.04a) so startup
 * order between them does not matter; RabbitMQ accepts a redeclaration of an existing
 * resource as long as the arguments match. Must stay identical to
 * {@code fuelbridge-ordering-service}'s {@code OrderEventsRabbitConfiguration}.
 */
@Configuration
public class OrderEventsRabbitConfiguration {

    public static final String EXCHANGE_NAME = "order-events";
    public static final String QUEUE_NAME = "notification-service.order-events";
    public static final String ROUTING_KEY_PATTERN = "#";

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue orderEventsQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding orderEventsBinding(Queue orderEventsQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderEventsQueue).to(orderEventsExchange).with(ROUTING_KEY_PATTERN);
    }

    /**
     * Producer and consumer are different classes in different modules with the same JSON
     * shape, so the converter must infer the target type from the listener's parameter
     * instead of trusting the {@code __TypeId__} header the producer would otherwise send.
     */
    @Bean
    public MessageConverter orderEventsMessageConverter() {
        var converter = new JacksonJsonMessageConverter();
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
