package com.halofuel.fuelbridge.platform.shared.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InventoryServiceClient extends StandaloneServiceClient {
    public InventoryServiceClient(@Value("${inventory.service.url}") String baseUrl) {
        super(baseUrl);
    }
}
