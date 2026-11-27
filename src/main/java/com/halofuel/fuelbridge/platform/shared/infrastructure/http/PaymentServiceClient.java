package com.halofuel.fuelbridge.platform.shared.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceClient extends StandaloneServiceClient {
    public PaymentServiceClient(@Value("${payment.service.url}") String baseUrl) {
        super(baseUrl);
    }
}
