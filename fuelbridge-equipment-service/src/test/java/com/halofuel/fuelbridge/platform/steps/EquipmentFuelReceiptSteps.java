package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.equipment.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EquipmentFuelReceiptSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    private String jwt;
    private Long equipmentId;
    private HttpStatusCode lastStatus;

    @Given("the buyer is authenticated")
    public void theBuyerIsAuthenticated() {
        this.jwt = tokens.generateToken("buyer");
    }

    @Given("company {int} has registered equipment {string} with a {int} liter tank at {int} liters")
    public void companyHasRegisteredEquipment(int companyId, String name, int tankCapacity, int currentLevel) {
        var body = new LinkedHashMap<String, Object>();
        body.put("name", name);
        body.put("equipmentType", "EXCAVATOR");
        body.put("licensePlate", "EQ-320-LIM");
        body.put("fuelType", "DIESEL");
        body.put("tankCapacity", tankCapacity);
        body.put("currentLevel", currentLevel);
        body.put("location", "Obra Villa El Salvador, Lima");
        body.put("status", "ACTIVE");
        body.put("autoRefill", true);
        body.put("refillThreshold", 25);
        body.put("lastRefillDate", "2026-10-08");
        body.put("companyId", companyId);
        var response = client().post().uri("/api/v1/equipment")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().toEntity(Map.class);
        assertEquals(201, response.getStatusCode().value());
        this.equipmentId = ((Number) response.getBody().get("id")).longValue();
    }

    @When("{int} liters are received for the equipment")
    public void litersAreReceivedForTheEquipment(int quantity) {
        receive(equipmentId, quantity);
    }

    @When("{int} liters are received for equipment {long}")
    public void litersAreReceivedForEquipment(int quantity, long unknownEquipmentId) {
        receive(unknownEquipmentId, quantity);
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus.value());
    }

    @Then("the equipment fuel level should be {int}")
    public void theEquipmentFuelLevelShouldBe(int expectedLevel) {
        var equipment = client().get().uri("/api/v1/equipment/{id}", equipmentId)
                .retrieve().body(Map.class);
        assertEquals(expectedLevel, ((Number) equipment.get("currentLevel")).intValue());
    }

    @When("the buyer deletes the equipment")
    public void theBuyerDeletesTheEquipment() {
        delete(equipmentId);
    }

    @When("the buyer deletes equipment {long}")
    public void theBuyerDeletesEquipment(long unknownEquipmentId) {
        delete(unknownEquipmentId);
    }

    @Then("the equipment should no longer exist")
    public void theEquipmentShouldNoLongerExist() {
        var missing = assertThrows(RestClientResponseException.class, () -> client().get()
                .uri("/api/v1/equipment/{id}", equipmentId).retrieve().toBodilessEntity());
        assertEquals(404, missing.getStatusCode().value());
    }

    private void delete(Long id) {
        try {
            lastStatus = client().delete().uri("/api/v1/equipment/{id}", id).retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode();
        }
    }

    private void receive(Long id, int quantity) {
        try {
            var response = client().post().uri("/api/v1/equipment/{id}/fuel-receipts", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantity", quantity))
                    .retrieve().toBodilessEntity();
            lastStatus = response.getStatusCode();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode();
        }
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .defaultHeader("Authorization", "Bearer " + jwt)
                .build();
    }
}
