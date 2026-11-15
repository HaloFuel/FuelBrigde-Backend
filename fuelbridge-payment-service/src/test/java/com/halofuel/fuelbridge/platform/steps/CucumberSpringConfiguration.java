package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgePaymentServiceApplication;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

/** Boots the payment service for Cucumber scenarios. Ordering is a remote service, but
 * none of the current scenarios exercise the mark-paid transition, so no stand-in is needed. */
@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgePaymentServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
