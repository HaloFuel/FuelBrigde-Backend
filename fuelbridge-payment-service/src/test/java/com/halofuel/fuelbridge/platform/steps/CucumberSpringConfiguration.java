package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgePaymentServiceApplication;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Boots the payment service for Cucumber scenarios. Ordering is a remote service, so the
 * mark-paid transition that completing a payment requests is served by {@link OrderingHttpStub}.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgePaymentServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @DynamicPropertySource
    static void remoteServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("ordering.service.url", OrderingHttpStub::baseUrl);
    }
}
