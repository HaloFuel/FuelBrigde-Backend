package com.halofuel.fuelbridge.platform.shared.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FulfillmentServiceClient extends StandaloneServiceClient {
    public FulfillmentServiceClient(@Value("${fulfillment.service.url}") String baseUrl) {
        super(baseUrl);
    }
}
