package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Driver;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DriverRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class DriverRegistrationSteps {

    // License numbers are a unique column and scenarios share the same H2
    // instance, so every fixture license gets a fresh suffix.
    private static final AtomicInteger LICENSE_SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DriverRepository driverRepository;

    private Long providerId;
    private String licenseNumber;
    private Driver otherDriver;
    private MvcResult response;

    @Given("provider {long} prepares a driver with an unused license number")
    public void providerPreparesADriverWithAnUnusedLicenseNumber(long providerId) {
        this.providerId = providerId;
        this.licenseNumber = nextLicenseNumber();
    }

    @Given("a driver with that license number is already registered")
    public void aDriverWithThatLicenseNumberIsAlreadyRegistered() {
        driverRepository.save(new Driver(providerId, "Luis", "Quispe", licenseNumber, "+51911222333",
                "luis.quispe@example.com", "AVAILABLE"));
    }

    @Given("another driver of the provider is registered")
    public void anotherDriverOfTheProviderIsRegistered() {
        otherDriver = driverRepository.save(new Driver(providerId, "Ana", "Torres", nextLicenseNumber(),
                "+51944555666", "ana.torres@example.com", "AVAILABLE"));
    }

    @When("the provider submits the driver registration")
    public void theProviderSubmitsTheDriverRegistration() throws Exception {
        response = mockMvc.perform(
                post("/api/v1/drivers").with(user("provider"))
                        .contentType(APPLICATION_JSON)
                        .content(driverBody(licenseNumber))
        ).andReturn();
    }

    @When("the provider changes that other driver to the duplicated license number")
    public void theProviderChangesThatOtherDriverToTheDuplicatedLicenseNumber() throws Exception {
        response = mockMvc.perform(
                put("/api/v1/drivers/{id}", otherDriver.getId()).with(user("provider"))
                        .contentType(APPLICATION_JSON)
                        .content(driverBody(licenseNumber))
        ).andReturn();
    }

    @Then("the driver registration response status code should be {int}")
    public void theDriverRegistrationResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the registered driver status should be {string}")
    public void theRegisteredDriverStatusShouldBe(String expectedStatus) throws Exception {
        jsonPath("$.licenseNumber").value(licenseNumber).match(response);
        jsonPath("$.status").value(expectedStatus).match(response);
    }

    @Then("the driver registration error code should be {string}")
    public void theDriverRegistrationErrorCodeShouldBe(String expectedCode) throws Exception {
        jsonPath("$.code").value(expectedCode).match(response);
    }

    private static String nextLicenseNumber() {
        return "DRV-LIC-%04d".formatted(LICENSE_SEQUENCE.incrementAndGet());
    }

    private String driverBody(String license) {
        return """
                {"providerId": %d, "firstName": "Carlos", "lastName": "Mamani", "licenseNumber": "%s",
                 "phoneNumber": "+51987654321", "email": "carlos.mamani@example.com"}
                """.formatted(providerId, license);
    }
}
