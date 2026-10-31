package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgeInventoryServiceApplication;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the inventory service for Cucumber scenarios. The inventory scenarios moved from the
 * monolith keep using MockMvc with an authenticated provider; Inventory has no outbound HTTP calls.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgeInventoryServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
