package com.halofuel.fuelbridge.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class FuelBridgeNotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuelBridgeNotificationServiceApplication.class, args);
    }
}
