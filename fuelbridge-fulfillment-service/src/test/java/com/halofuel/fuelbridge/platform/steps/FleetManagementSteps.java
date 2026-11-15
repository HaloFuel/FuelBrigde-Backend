package com.halofuel.fuelbridge.platform.steps;

import com.jayway.jsonpath.JsonPath;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Covers listing, lookup, update and deletion of drivers (TS-18) and vehicles (TS-19).
 * Drivers are referred to by license number and vehicles by plate; the ids returned
 * by the API are tracked here. Registration and duplicates are covered elsewhere.
 */
public class FleetManagementSteps {

    @Autowired
    private MockMvc mockMvc;

    private final Map<String, Long> driverIds = new HashMap<>();
    private final Map<String, Long> vehicleIds = new HashMap<>();
    private final Map<String, Long> providerOf = new HashMap<>();

    private int lastStatus;
    private String lastBody;

    // ---------- drivers ----------

    @Given("provider {int} has registered the driver with license {string}")
    public void providerHasRegisteredTheDriver(int providerId, String license) throws Exception {
        perform(post("/api/v1/drivers").contentType(MediaType.APPLICATION_JSON)
                .content(driverBody(providerId, license, "+51911222333")));
        assertEquals(201, lastStatus, "The driver could not be registered: " + lastBody);
        driverIds.put(license, idOfLastResponse());
        providerOf.put(license, (long) providerId);
    }

    @When("the drivers of provider {int} are listed")
    public void theDriversOfProviderAreListed(int providerId) throws Exception {
        perform(get("/api/v1/drivers").param("providerId", String.valueOf(providerId)));
    }

    @When("the driver with license {string} is requested")
    public void theDriverIsRequested(String license) throws Exception {
        perform(get("/api/v1/drivers/{id}", driverId(license)));
    }

    @When("driver {long} is requested")
    public void driverIsRequested(long id) throws Exception {
        perform(get("/api/v1/drivers/{id}", id));
    }

    @When("the driver with license {string} changes the phone number to {string}")
    public void theDriverChangesThePhoneNumber(String license, String phone) throws Exception {
        perform(put("/api/v1/drivers/{id}", driverId(license)).contentType(MediaType.APPLICATION_JSON)
                .content(driverBody(providerOf.get(license), license, phone)));
    }

    @When("the driver with license {string} is deleted")
    public void theDriverIsDeleted(String license) throws Exception {
        perform(delete("/api/v1/drivers/{id}", driverId(license)));
    }

    @When("driver {long} is deleted")
    public void driverIsDeleted(long id) throws Exception {
        perform(delete("/api/v1/drivers/{id}", id));
    }

    @Then("the listed drivers have licenses {string}")
    public void theListedDriversHaveLicenses(String licenses) {
        assertEquals(csv(licenses), valuesOfList("$[*].licenseNumber"));
    }

    @Then("the returned driver has license {string}")
    public void theReturnedDriverHasLicense(String license) {
        assertEquals(license, JsonPath.read(lastBody, "$.licenseNumber"));
    }

    @Then("the returned driver has the phone number {string}")
    public void theReturnedDriverHasThePhoneNumber(String phone) {
        assertEquals(phone, JsonPath.read(lastBody, "$.phoneNumber"));
    }

    @Then("the driver with license {string} no longer exists")
    public void theDriverNoLongerExists(String license) throws Exception {
        perform(get("/api/v1/drivers/{id}", driverId(license)));
        assertEquals(404, lastStatus);
    }

    // ---------- vehicles ----------

    @Given("provider {int} has registered the vehicle with plate {string}")
    public void providerHasRegisteredTheVehicle(int providerId, String plate) throws Exception {
        perform(post("/api/v1/vehicles").contentType(MediaType.APPLICATION_JSON)
                .content(vehicleBody(providerId, plate, 10000.0)));
        assertEquals(201, lastStatus, "The vehicle could not be registered: " + lastBody);
        vehicleIds.put(plate, idOfLastResponse());
        providerOf.put(plate, (long) providerId);
    }

    @When("the vehicles of provider {int} are listed")
    public void theVehiclesOfProviderAreListed(int providerId) throws Exception {
        perform(get("/api/v1/vehicles").param("providerId", String.valueOf(providerId)));
    }

    @When("the vehicle with plate {string} is requested")
    public void theVehicleIsRequested(String plate) throws Exception {
        perform(get("/api/v1/vehicles/{id}", vehicleId(plate)));
    }

    @When("vehicle {long} is requested")
    public void vehicleIsRequested(long id) throws Exception {
        perform(get("/api/v1/vehicles/{id}", id));
    }

    @When("the vehicle with plate {string} changes its capacity to {double} liters")
    public void theVehicleChangesItsCapacity(String plate, double capacity) throws Exception {
        perform(put("/api/v1/vehicles/{id}", vehicleId(plate)).contentType(MediaType.APPLICATION_JSON)
                .content(vehicleBody(providerOf.get(plate), plate, capacity)));
    }

    @When("the vehicle with plate {string} is deleted")
    public void theVehicleIsDeleted(String plate) throws Exception {
        perform(delete("/api/v1/vehicles/{id}", vehicleId(plate)));
    }

    @When("vehicle {long} is deleted")
    public void vehicleIsDeleted(long id) throws Exception {
        perform(delete("/api/v1/vehicles/{id}", id));
    }

    @Then("the listed vehicles have plates {string}")
    public void theListedVehiclesHavePlates(String plates) {
        assertEquals(csv(plates), valuesOfList("$[*].licensePlate"));
    }

    @Then("the returned vehicle has plate {string}")
    public void theReturnedVehicleHasPlate(String plate) {
        assertEquals(plate, JsonPath.read(lastBody, "$.licensePlate"));
    }

    @Then("the returned vehicle has a capacity of {double} liters")
    public void theReturnedVehicleHasACapacity(double capacity) {
        assertEquals(capacity, ((Number) JsonPath.read(lastBody, "$.capacity")).doubleValue());
    }

    @Then("the vehicle with plate {string} no longer exists")
    public void theVehicleNoLongerExists(String plate) throws Exception {
        perform(get("/api/v1/vehicles/{id}", vehicleId(plate)));
        assertEquals(404, lastStatus);
    }

    // ---------- shared ----------

    @Then("the fleet service responds with status {int}")
    public void theFleetServiceRespondsWithStatus(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus, "Unexpected response: " + lastBody);
    }

    private void perform(MockHttpServletRequestBuilder request) throws Exception {
        var response = mockMvc.perform(request.with(user("provider"))).andReturn().getResponse();
        lastStatus = response.getStatus();
        lastBody = response.getContentAsString();
    }

    private Long idOfLastResponse() {
        return ((Number) JsonPath.read(lastBody, "$.id")).longValue();
    }

    private Set<String> valuesOfList(String path) {
        List<String> values = JsonPath.read(lastBody, path);
        return new HashSet<>(values);
    }

    private static Set<String> csv(String values) {
        return Arrays.stream(values.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    private Long driverId(String license) {
        var id = driverIds.get(license);
        assertNotNull(id, "No driver with license " + license + " was registered in this scenario");
        return id;
    }

    private Long vehicleId(String plate) {
        var id = vehicleIds.get(plate);
        assertNotNull(id, "No vehicle with plate " + plate + " was registered in this scenario");
        return id;
    }

    private static String driverBody(long providerId, String license, String phone) {
        return """
                {"providerId": %d, "firstName": "Carlos", "lastName": "Mamani", "licenseNumber": "%s",
                 "phoneNumber": "%s", "email": "conductor@halofuel.pe", "status": "AVAILABLE"}"""
                .formatted(providerId, license, phone);
    }

    private static String vehicleBody(long providerId, String plate, double capacity) {
        return """
                {"providerId": %d, "licensePlate": "%s", "brand": "Volvo", "model": "FMX Cisterna",
                 "capacity": %s, "unit": "LITERS", "status": "AVAILABLE"}"""
                .formatted(providerId, plate, capacity);
    }
}
