package com.halofuel.fuelbridge.platform.reporting.infrastructure.http;

import com.halofuel.fuelbridge.platform.reporting.application.outboundservices.AnalyticsDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

/** Queries existing REST endpoints without reading the monolith's schema. */
@Component
public class FuelBridgeAnalyticsDataSource implements AnalyticsDataSource {
    private final RestClient client;

    public FuelBridgeAnalyticsDataSource(@Value("${fuelbridge.api.url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public List<OrderSnapshot> allOrders() {
        return get("/api/v1/fuel-orders", OrderSnapshot[].class);
    }

    @Override
    public List<OrderSnapshot> ordersByCompany(Long companyId) {
        return get("/api/v1/fuel-orders/company/" + companyId, OrderSnapshot[].class);
    }

    @Override
    public List<OrderSnapshot> ordersByProvider(Long providerId) {
        return get("/api/v1/fuel-orders/provider/" + providerId, OrderSnapshot[].class);
    }

    @Override
    public List<PaymentSnapshot> allPayments() {
        return get("/api/v1/payments", PaymentSnapshot[].class);
    }

    @Override
    public List<PaymentSnapshot> paymentsByCompany(Long companyId) {
        return get("/api/v1/payments/company/" + companyId, PaymentSnapshot[].class);
    }

    @Override
    public List<DeliverySnapshot> allDeliveries() {
        return get("/api/v1/deliveries", DeliverySnapshot[].class);
    }

    private <T> List<T> get(String path, Class<T[]> type) {
        var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        var authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        try {
            var rows = client.get().uri(path).header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(type);
            if (rows == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty analytics source response");
            return Arrays.asList(rows);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Analytics source unavailable", ex);
        }
    }
}
