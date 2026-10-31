package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgeEquipmentServiceApplication;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the equipment service for Cucumber scenarios. Equipment has no outbound
 * HTTP dependency: it only validates the JWT locally and uses its own schema.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgeEquipmentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
