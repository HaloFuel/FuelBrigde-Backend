package com.halofuel.fuelbridge.platform.fulfillment.infrastructure.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.FuelOrders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Calls the Ordering endpoints of the FuelBridge Platform. The platform resolves
 * the JWT subject against its user store, so the caller's own token is relayed
 * (the same approach Reporting uses) instead of a service token.
 */
@Component
public class OrderingServiceFuelOrders implements FuelOrders {

    private static final String ORDERS = "/api/v1/fuel-orders/{orderId}";

    private final RestClient client;

    public OrderingServiceFuelOrders(@Value("${ordering.service.url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public Optional<FuelOrderSnapshot> findById(Long orderId) {
        try {
            var order = client.get().uri(ORDERS, orderId)
                    .headers(this::relayAuthorization)
                    .retrieve().body(FuelOrderResponse.class);
            if (order == null) {
                throw new OrderingUnavailableException("Empty response from Ordering for order " + orderId, null);
            }
            return Optional.of(order.toSnapshot());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new OrderingUnavailableException("Ordering answered " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new OrderingUnavailableException("Ordering service unavailable", ex);
        }
    }

    @Override
    public TransitionOutcome dispatch(Long orderId) {
        return transition(ORDERS + "/dispatch", orderId);
    }

    @Override
    public TransitionOutcome receive(Long orderId) {
        return transition(ORDERS + "/receive", orderId);
    }

    private TransitionOutcome transition(String path, Long orderId) {
        try {
            client.post().uri(path, orderId)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FuelOrderResponse(Long id, Long providerId, Long fuelProductId, Long equipmentId,
                                    Double requestedQuantity, String status) {

        FuelOrderSnapshot toSnapshot() {
            return new FuelOrderSnapshot(id, providerId, fuelProductId, equipmentId, requestedQuantity, status);
        }
    }
}
