package com.halofuel.fuelbridge.platform.shared.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EquipmentServiceClient extends StandaloneServiceClient {
    public EquipmentServiceClient(@Value("${equipment.service.url}") String baseUrl) {
        super(baseUrl);
    }
}
