package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgeReportingServiceApplication;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgeReportingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("fuelbridge.api.url", AnalyticsSourceStub::baseUrl);
    }
}
