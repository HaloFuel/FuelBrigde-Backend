package com.halofuel.fuelbridge.platform.ordering.infrastructure.notification;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.CompanyUsers;
import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.IntegrationEventPublisher;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.outbox.FuelOrderOutboxRecorder;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.security.configuration.ServiceTokenConfiguration;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter of {@link IntegrationEventPublisher} that turns each Ordering event into a
 * notification for the buyer: the buyer's user is resolved in Identity &amp; Access and the
 * notification is posted to the Notification service with Ordering's service token. Any
 * failure is thrown back to the outbox relay, which retries the event later.
 */
@Component
public class NotificationServiceEventPublisher implements IntegrationEventPublisher {

    private final CompanyUsers companyUsers;
    private final ServiceTokenIssuer serviceTokens;
    private final RestClient client;

    public NotificationServiceEventPublisher(CompanyUsers companyUsers, ServiceTokenIssuer serviceTokens,
                                             @Value("${notification.service.url}") String baseUrl) {
        this.companyUsers = companyUsers;
        this.serviceTokens = serviceTokens;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public void publish(OrderIntegrationEvent event) {
        var notification = switch (event.eventType()) {
            case FuelOrderOutboxRecorder.ORDER_CONFIRMED -> new Template("ORDER_CONFIRMED", "Pedido confirmado", "confirmado");
            case FuelOrderOutboxRecorder.ORDER_CANCELLED -> new Template("ORDER_CANCELLED", "Pedido cancelado", "cancelado");
            case FuelOrderOutboxRecorder.ORDER_DISPATCHED -> new Template("ORDER_DISPATCHED", "Pedido despachado", "despachado");
            default -> throw new IllegalArgumentException("Unknown Ordering event type " + event.eventType());
        };
        // A company without a user has nobody to notify: the event is delivered with no request.
        var userId = companyUsers.findUserIdByCompanyId(event.companyId());
        if (userId.isEmpty()) {
            return;
        }
        String jwt = serviceTokens.issueFor(ServiceTokenConfiguration.SERVICE_NAME);
        client.post().uri("/api/v1/notifications")
                .headers(headers -> {
                    headers.setBearerAuth(jwt);
                    headers.set("Idempotency-Key", event.eventId());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(new NotificationRequest(userId.get(), notification.type(), notification.title(),
                        "El pedido número " + event.orderId() + " ha sido " + notification.status() + ".",
                        event.orderId()))
                .retrieve().toBodilessEntity();
    }

    private record Template(String type, String title, String status) {}

    public record NotificationRequest(Long userId, String type, String title, String message, Long referenceId) {}
}
