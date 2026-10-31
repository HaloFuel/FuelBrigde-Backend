package com.halofuel.fuelbridge.platform.fulfillment.infrastructure.http;

import com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices.EquipmentFuelReceipts;
import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.tokens.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Calls POST /api/v1/equipment/{id}/fuel-receipts on the standalone Equipment service. */
@Component
public class EquipmentServiceFuelReceipts implements EquipmentFuelReceipts {
    private final TokenService tokenService;
    private final RestClient client;

    public EquipmentServiceFuelReceipts(TokenService tokenService,
                                        @Value("${equipment.service.url}") String baseUrl) {
        this.tokenService = tokenService;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public FuelReceiptOutcome register(Long equipmentId, Double quantity) {
        // Service identity: the Equipment service validates it locally with the shared JWT secret.
        String jwt = tokenService.generateToken("fuelbridge-fulfillment");
        try {
            client.post().uri("/api/v1/equipment/{id}/fuel-receipts", equipmentId)
                    .headers(headers -> headers.setBearerAuth(jwt))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FuelReceiptRequest(quantity))
                    .retrieve().toBodilessEntity();
            return FuelReceiptOutcome.REGISTERED;
        } catch (RestClientResponseException ex) {
            return ex.getStatusCode().value() == 404
                    ? FuelReceiptOutcome.EQUIPMENT_NOT_FOUND
                    : FuelReceiptOutcome.UNAVAILABLE;
        } catch (RestClientException ex) {
            return FuelReceiptOutcome.UNAVAILABLE;
        }
    }

    public record FuelReceiptRequest(Double quantity) {}
}
