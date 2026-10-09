package com.halofuel.fuelbridge.platform.shared.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportingServiceClient extends StandaloneServiceClient {
    public ReportingServiceClient(@Value("${reporting.service.url}") String baseUrl) {
        super(baseUrl);
    }
}
