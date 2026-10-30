package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Vehicle;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.VehicleRepository;

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

public class VehicleRegistrationSteps {

    // License plates are a unique column and scenarios share the same H2
    // instance, so every fixture plate gets a fresh suffix.
    private static final AtomicInteger PLATE_SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    private Long providerId;
    private String licensePlate;
    private Vehicle otherVehicle;
    private MvcResult response;

    @Given("provider {long} prepares a vehicle with an unused license plate")
    public void providerPreparesAVehicleWithAnUnusedLicensePlate(long providerId) {
        this.providerId = providerId;
        this.licensePlate = nextPlate();
    }

    @Given("a vehicle with that license plate is already registered")
    public void aVehicleWithThatLicensePlateIsAlreadyRegistered() {
        vehicleRepository.save(new Vehicle(providerId, licensePlate, "Scania", "P410", 9000.0,
                "LITERS", "AVAILABLE"));
    }

    @Given("another vehicle of the provider is registered")
    public void anotherVehicleOfTheProviderIsRegistered() {
        otherVehicle = vehicleRepository.save(new Vehicle(providerId, nextPlate(), "Volvo", "FM 460",
                12000.0, "LITERS", "AVAILABLE"));
    }

    @When("the provider submits the vehicle registration")
    public void theProviderSubmitsTheVehicleRegistration() throws Exception {
        response = mockMvc.perform(
                post("/api/v1/vehicles").with(user("provider"))
                        .contentType(APPLICATION_JSON)
                        .content(vehicleBody(licensePlate))
        ).andReturn();
    }

    @When("the provider changes that other vehicle to the duplicated license plate")
    public void theProviderChangesThatOtherVehicleToTheDuplicatedLicensePlate() throws Exception {
        response = mockMvc.perform(
                put("/api/v1/vehicles/{id}", otherVehicle.getId()).with(user("provider"))
                        .contentType(APPLICATION_JSON)
                        .content(vehicleBody(licensePlate))
        ).andReturn();
    }

    @Then("the vehicle registration response status code should be {int}")
    public void theVehicleRegistrationResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the registered vehicle status should be {string}")
    public void theRegisteredVehicleStatusShouldBe(String expectedStatus) throws Exception {
        jsonPath("$.licensePlate").value(licensePlate).match(response);
        jsonPath("$.status").value(expectedStatus).match(response);
    }

    @Then("the vehicle registration error code should be {string}")
    public void theVehicleRegistrationErrorCodeShouldBe(String expectedCode) throws Exception {
        jsonPath("$.code").value(expectedCode).match(response);
    }

    private static String nextPlate() {
        return "VRG-%04d".formatted(PLATE_SEQUENCE.incrementAndGet());
    }

    private String vehicleBody(String plate) {
        return """
                {"providerId": %d, "licensePlate": "%s", "brand": "Volvo", "model": "FH 540",
                 "capacity": 12000, "unit": "LITERS"}
                """.formatted(providerId, plate);
    }
}
