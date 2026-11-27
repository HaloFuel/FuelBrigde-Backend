package com.halofuel.fuelbridge.platform.ordering.infrastructure.http;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.FuelProductCatalog;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.security.configuration.ServiceTokenConfiguration;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/** Reads GET /api/v1/fuel-products/{id} from the standalone Inventory service. */
@Component
public class InventoryServiceFuelProductCatalog implements FuelProductCatalog {
    private final ServiceTokenIssuer serviceTokens;
    private final RestClient client;

    public InventoryServiceFuelProductCatalog(ServiceTokenIssuer serviceTokens,
                                              @Value("${inventory.service.url}") String baseUrl) {
        this.serviceTokens = serviceTokens;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public Optional<FuelProductSnapshot> findById(Long fuelProductId) {
        // Service identity: Inventory validates it locally with the shared JWT secret.
        String jwt = serviceTokens.issueFor(ServiceTokenConfiguration.SERVICE_NAME);
        try {
            return Optional.ofNullable(client.get().uri("/api/v1/fuel-products/{id}", fuelProductId)
                    .headers(headers -> headers.setBearerAuth(jwt))
                    .retrieve().body(FuelProductSnapshot.class));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) return Optional.empty();
            throw new IllegalStateException("Inventory service unavailable", ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Inventory service unavailable", ex);
        }
    }
}
