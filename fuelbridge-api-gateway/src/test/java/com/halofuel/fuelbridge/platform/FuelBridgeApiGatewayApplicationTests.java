package com.halofuel.fuelbridge.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "PLATFORM_SERVICE_URL=http://localhost:9999")
class FuelBridgeApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
