package com.halofuel.fuelbridge.platform.shared.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CatalogServiceClient extends StandaloneServiceClient {
    public CatalogServiceClient(@Value("${catalog.service.url}") String baseUrl) {
        super(baseUrl);
    }
}
