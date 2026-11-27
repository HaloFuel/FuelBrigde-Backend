package com.halofuel.fuelbridge.platform.payment.infrastructure.http;

import com.halofuel.fuelbridge.platform.payment.application.outboundservices.FuelOrders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Calls the Ordering endpoints of the FuelBridge Platform. The platform resolves
 * the JWT subject against its user store, so the caller's own token is relayed
 * (the same approach Reporting and Fulfillment use) instead of a service token.
 */
@Component
public class OrderingServiceFuelOrders implements FuelOrders {

    private static final String MARK_PAID = "/api/v1/fuel-orders/{orderId}/mark-paid";

    private final RestClient client;

    public OrderingServiceFuelOrders(@Value("${ordering.service.url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public TransitionOutcome markPaid(Long orderId) {
        try {
            client.post().uri(MARK_PAID, orderId)
                    .headers(this::relayAuthorization)
                    .retrieve().toBodilessEntity();
            return TransitionOutcome.APPLIED;
        } catch (RestClientResponseException ex) {
            return switch (ex.getStatusCode().value()) {
                case 409 -> TransitionOutcome.REJECTED;
                case 404 -> TransitionOutcome.ORDER_NOT_FOUND;
                default -> TransitionOutcome.UNAVAILABLE;
            };
        } catch (RestClientException ex) {
            return TransitionOutcome.UNAVAILABLE;
        }
    }

    private void relayAuthorization(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            var authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
    }
}
