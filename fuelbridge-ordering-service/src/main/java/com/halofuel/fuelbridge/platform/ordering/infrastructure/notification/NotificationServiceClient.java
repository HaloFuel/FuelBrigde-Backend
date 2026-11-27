package com.halofuel.fuelbridge.platform.ordering.infrastructure.notification;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.CompanyUsers;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderCancelledEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderConfirmedEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderDispatchedEvent;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.security.configuration.ServiceTokenConfiguration;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Sends the order notifications to the standalone Notification service. The buyer's user
 * is resolved in Identity &amp; Access. A notification is a side effect: when IAM or
 * Notification cannot be reached the failure is logged and the order transition is kept.
 */
@Component
public class NotificationServiceClient {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClient.class);

    private final CompanyUsers companyUsers;
    private final ServiceTokenIssuer serviceTokens;
    private final RestClient client;

    public NotificationServiceClient(CompanyUsers companyUsers, ServiceTokenIssuer serviceTokens,
                                     @Value("${notification.service.url}") String baseUrl) {
        this.companyUsers = companyUsers;
        this.serviceTokens = serviceTokens;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @EventListener
    public void on(FuelOrderConfirmedEvent event) {
        send(event.companyId(), event.orderId(), "ORDER_CONFIRMED", "Pedido confirmado", "confirmado");
    }

    @EventListener
    public void on(FuelOrderCancelledEvent event) {
        send(event.companyId(), event.orderId(), "ORDER_CANCELLED", "Pedido cancelado", "cancelado");
    }

    @EventListener
    public void on(FuelOrderDispatchedEvent event) {
        send(event.companyId(), event.orderId(), "ORDER_DISPATCHED", "Pedido despachado", "despachado");
    }

    private void send(Long companyId, Long orderId, String type, String title, String status) {
        try {
            companyUsers.findUserIdByCompanyId(companyId).ifPresent(userId -> {
                String jwt = serviceTokens.issueFor(ServiceTokenConfiguration.SERVICE_NAME);
                client.post().uri("/api/v1/notifications")
                        .headers(headers -> headers.setBearerAuth(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new NotificationRequest(userId, type, title,
                                "El pedido número " + orderId + " ha sido " + status + ".", orderId))
                        .retrieve().toBodilessEntity();
            });
        } catch (RuntimeException ex) {
            log.warn("{} notification for order {} was not sent: {}", type, orderId, ex.getMessage());
        }
    }

    public record NotificationRequest(Long userId, String type, String title, String message, Long referenceId) {}
}
