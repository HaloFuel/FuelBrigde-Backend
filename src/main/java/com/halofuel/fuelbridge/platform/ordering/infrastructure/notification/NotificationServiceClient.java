package com.halofuel.fuelbridge.platform.ordering.infrastructure.notification;

import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;
import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderCancelledEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderConfirmedEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderDispatchedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Synchronous HTTP replacement for the former in-process notification listener. */
@Component
public class NotificationServiceClient {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RestClient client;

    public NotificationServiceClient(UserRepository userRepository, TokenService tokenService,
                                     @Value("${notification.service.url}") String baseUrl) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
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
        userRepository.findByCompanyId(companyId).ifPresent(user -> {
            // Service identity works for both HTTP requests and background event publishers.
            String jwt = tokenService.generateToken("fuelbridge-notification-publisher");
            client.post().uri("/api/v1/notifications")
                    .headers(headers -> headers.setBearerAuth(jwt))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NotificationRequest(user.getId(), type, title,
                            "El pedido número " + orderId + " ha sido " + status + ".", orderId))
                    .retrieve().toBodilessEntity();
        });
    }

    public record NotificationRequest(Long userId, String type, String title, String message, Long referenceId) {}
}
