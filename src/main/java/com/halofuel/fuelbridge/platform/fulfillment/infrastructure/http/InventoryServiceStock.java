package com.halofuel.fuelbridge.platform.fulfillment.infrastructure.http;

import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.InventoryStock;
import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.tokens.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Calls the stock-reservations and stock-releases endpoints of the standalone Inventory service. */
@Component
public class InventoryServiceStock implements InventoryStock {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryServiceStock.class);
    private final TokenService tokenService;
    private final RestClient client;

    public InventoryServiceStock(TokenService tokenService,
                                 @Value("${inventory.service.url}") String baseUrl) {
        this.tokenService = tokenService;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public StockReservationOutcome reserve(Long fuelProductId, Double quantity) {
        try {
            post("/api/v1/fuel-products/{id}/stock-reservations", fuelProductId, quantity);
            return StockReservationOutcome.RESERVED;
        } catch (RestClientResponseException ex) {
            return switch (ex.getStatusCode().value()) {
                case 409 -> StockReservationOutcome.INSUFFICIENT_STOCK;
                case 404 -> StockReservationOutcome.PRODUCT_NOT_FOUND;
                default -> StockReservationOutcome.UNAVAILABLE;
            };
        } catch (RestClientException ex) {
            return StockReservationOutcome.UNAVAILABLE;
        }
    }

    @Override
    public void release(Long fuelProductId, Double quantity) {
        try {
            post("/api/v1/fuel-products/{id}/stock-releases", fuelProductId, quantity);
        } catch (RestClientException ex) {
            LOGGER.error("Stock release failed for fuel product {} ({} units); manual reconciliation required",
                    fuelProductId, quantity, ex);
        }
    }

    private void post(String path, Long fuelProductId, Double quantity) {
        // Service identity: Inventory validates it locally with the shared JWT secret.
        String jwt = tokenService.generateToken("fuelbridge-fulfillment");
        client.post().uri(path, fuelProductId)
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StockQuantityRequest(quantity))
                .retrieve().toBodilessEntity();
    }

    public record StockQuantityRequest(Double quantity) {}
}
