package com.halofuel.fuelbridge.platform.notification.infrastructure.broker;

import com.halofuel.fuelbridge.platform.notification.application.commandservices.NotificationCommandService;
import com.halofuel.fuelbridge.platform.notification.application.outboundservices.CompanyUsers;
import com.halofuel.fuelbridge.platform.notification.domain.model.commands.CreateNotificationCommand;
import com.halofuel.fuelbridge.platform.notification.domain.model.valueobjects.NotificationType;
import com.halofuel.fuelbridge.platform.notification.infrastructure.persistence.jpa.entities.ProcessedEventRecord;
import com.halofuel.fuelbridge.platform.notification.infrastructure.persistence.jpa.repositories.ProcessedEventRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Consumes Ordering's events off the "order-events" broker (T10.04a) and turns each one
 * into a notification for the buyer, mirroring what
 * {@code fuelbridge-ordering-service}'s HTTP {@code NotificationServiceEventPublisher}
 * already does for the synchronous path. Delivery is at least once, so the same
 * {@code eventId} can arrive more than once; {@link ProcessedEventRecordRepository}
 * deduplicates it, which the synchronous HTTP endpoint still does not do.
 */
@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    // Must match com.halofuel.fuelbridge.platform.ordering.infrastructure.outbox.FuelOrderOutboxRecorder
    // in fuelbridge-ordering-service; Notification does not depend on that module.
    private static final String ORDER_CONFIRMED = "FuelOrderConfirmed";
    private static final String ORDER_CANCELLED = "FuelOrderCancelled";
    private static final String ORDER_DISPATCHED = "FuelOrderDispatched";

    private final ProcessedEventRecordRepository processedEvents;
    private final CompanyUsers companyUsers;
    private final NotificationCommandService notificationCommandService;

    public OrderEventsListener(ProcessedEventRecordRepository processedEvents, CompanyUsers companyUsers,
                               NotificationCommandService notificationCommandService) {
        this.processedEvents = processedEvents;
        this.companyUsers = companyUsers;
        this.notificationCommandService = notificationCommandService;
    }

    @RabbitListener(queues = OrderEventsRabbitConfiguration.QUEUE_NAME)
    public void onOrderEvent(OrderIntegrationEventMessage event) {
        if (processedEvents.existsByEventId(event.eventId())) {
            log.info("Discarding already processed order event {} ({})", event.eventId(), event.eventType());
            return;
        }
        var userId = companyUsers.findUserIdByCompanyId(event.companyId());
        if (userId.isEmpty()) {
            log.info("Discarding order event {} ({}): company {} has no IAM user", event.eventId(),
                    event.eventType(), event.companyId());
            return;
        }
        var template = templateFor(event.eventType());
        handle(event, userId.get(), template);
    }

    @Transactional
    public void handle(OrderIntegrationEventMessage event, Long userId, Template template) {
        var command = new CreateNotificationCommand(userId, template.type(), template.title(),
                "El pedido número " + event.orderId() + " ha sido " + template.status() + ".", event.orderId());
        notificationCommandService.handle(command);
        processedEvents.save(new ProcessedEventRecord(event.eventId(), Instant.now()));
    }

    private Template templateFor(String eventType) {
        return switch (eventType) {
            case ORDER_CONFIRMED -> new Template(NotificationType.ORDER_CONFIRMED, "Pedido confirmado", "confirmado");
            case ORDER_CANCELLED -> new Template(NotificationType.ORDER_CANCELLED, "Pedido cancelado", "cancelado");
            case ORDER_DISPATCHED -> new Template(NotificationType.ORDER_DISPATCHED, "Pedido despachado", "despachado");
            default -> throw new IllegalArgumentException("Unknown Ordering event type " + eventType);
        };
    }

    private record Template(NotificationType type, String title, String status) {}

    public record OrderIntegrationEventMessage(String eventId, String eventType, Long orderId, Long companyId,
                                               Instant occurredAt) {}
}
