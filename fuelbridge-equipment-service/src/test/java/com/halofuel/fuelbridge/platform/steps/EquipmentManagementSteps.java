package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.equipment.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers equipment registration, updates and queries (TS-23) and the favorite
 * provider assignment (TS-24). Equipment is referred to by name in the scenarios;
 * the ids returned by the API are tracked here.
 */
public class EquipmentManagementSteps {

    private static final ParameterizedTypeReference<Map<String, Object>> EQUIPMENT =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> EQUIPMENT_LIST =
            new ParameterizedTypeReference<>() { };

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    private final Map<String, Long> idsByName = new HashMap<>();

    private int lastStatus;
    private Map<String, Object> lastEquipment;
    private List<Map<String, Object>> lastList;
    private String lastErrorBody;

    @Given("company {int} owns the equipment {string}")
    public void companyOwnsTheEquipment(int companyId, String name) {
        register(companyId, name);
        assertEquals(201, lastStatus, "The equipment could not be registered");
    }

    @When("company {int} registers the equipment {string}")
    public void companyRegistersTheEquipment(int companyId, String name) {
        register(companyId, name);
    }

    @When("the equipment {string} is renamed to {string} with {int} liters")
    public void theEquipmentIsRenamed(String name, String newName, int currentLevel) {
        var body = equipmentBody(newName);
        body.put("currentLevel", currentLevel);
        exchangeOne(() -> client().post().uri("/api/v1/equipment/{id}/update", idOf(name))
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(EQUIPMENT));
    }

    @When("I list all equipment")
    public void iListAllEquipment() {
        exchangeList("/api/v1/equipment", null);
    }

    @When("I look up the equipment {string}")
    public void iLookUpTheEquipment(String name) {
        exchangeOne(() -> client().get().uri("/api/v1/equipment/{id}", idOf(name))
                .retrieve().toEntity(EQUIPMENT));
    }

    @When("I list the equipment of company {int}")
    public void iListTheEquipmentOfCompany(int companyId) {
        exchangeList("/api/v1/equipment/company/{id}", companyId);
    }

    @When("provider {int} is set as favorite for the equipment {string}")
    public void providerIsSetAsFavoriteForTheEquipment(int providerId, String name) {
        assignFavorite(idOf(name), providerId);
    }

    @When("provider {int} is set as favorite for equipment {long}")
    public void providerIsSetAsFavoriteForEquipment(int providerId, long equipmentId) {
        assignFavorite(equipmentId, providerId);
    }

    @Then("the equipment service responds with status {int}")
    public void theEquipmentServiceRespondsWithStatus(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus);
    }

    @Then("the returned equipment is named {string}")
    public void theReturnedEquipmentIsNamed(String name) {
        assertNotNull(lastEquipment, "No equipment was returned");
        assertEquals(name, lastEquipment.get("name"));
    }

    @Then("the returned equipment holds {int} liters")
    public void theReturnedEquipmentHolds(int liters) {
        assertNotNull(lastEquipment, "No equipment was returned");
        assertEquals(liters, ((Number) lastEquipment.get("currentLevel")).intValue());
    }

    @Then("the listed equipment is {string}")
    public void theListedEquipmentIs(String names) {
        assertNotNull(lastList, "No list was returned");
        Set<String> expected = Arrays.stream(names.split(",")).map(String::trim).collect(Collectors.toSet());
        Set<String> actual = lastList.stream().map(e -> (String) e.get("name")).collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Then("the favorite provider of {string} is {int}")
    public void theFavoriteProviderIs(String name, int providerId) {
        assertEquals((long) providerId, favoriteProviderOf(name));
    }

    @Then("{string} has no favorite provider")
    public void hasNoFavoriteProvider(String name) {
        assertNull(favoriteProviderOf(name));
    }

    @Then("the error code is {string}")
    public void theErrorCodeIs(String code) {
        assertNotNull(lastErrorBody, "The response had no error body");
        var error = JsonMapper.builder().build().readValue(lastErrorBody, Map.class);
        assertEquals(code, error.get("code"));
    }

    private void register(int companyId, String name) {
        var body = equipmentBody(name);
        body.put("companyId", companyId);
        exchangeOne(() -> client().post().uri("/api/v1/equipment")
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(EQUIPMENT));
        if (lastStatus == 201) {
            idsByName.put(name, ((Number) lastEquipment.get("id")).longValue());
        }
    }

    private void assignFavorite(Long equipmentId, int providerId) {
        exchangeOne(() -> client().post().uri("/api/v1/equipment/{id}/favorite-provider", equipmentId)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("providerId", providerId))
                .retrieve().toEntity(EQUIPMENT));
    }

    private Long favoriteProviderOf(String name) {
        var equipment = client().get().uri("/api/v1/equipment/{id}", idOf(name)).retrieve().body(EQUIPMENT);
        assertNotNull(equipment);
        var favorite = (Number) equipment.get("favoriteProviderId");
        return favorite == null ? null : favorite.longValue();
    }

    private void exchangeOne(java.util.function.Supplier<org.springframework.http.ResponseEntity<Map<String, Object>>> call) {
        try {
            var response = call.get();
            lastStatus = response.getStatusCode().value();
            lastEquipment = response.getBody();
            lastErrorBody = null;
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
            lastEquipment = null;
            lastErrorBody = ex.getResponseBodyAsString();
        }
    }

    private void exchangeList(String uri, Integer id) {
        try {
            var spec = id == null ? client().get().uri(uri) : client().get().uri(uri, id);
            var response = spec.retrieve().toEntity(EQUIPMENT_LIST);
            lastStatus = response.getStatusCode().value();
            lastList = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
            lastList = null;
        }
    }

    private static Map<String, Object> equipmentBody(String name) {
        var body = new LinkedHashMap<String, Object>();
        body.put("name", name);
        body.put("equipmentType", "EXCAVATOR");
        body.put("licensePlate", "EQ-" + Math.abs(name.hashCode() % 1000));
        body.put("fuelType", "DIESEL");
        body.put("tankCapacity", 400);
        body.put("currentLevel", 80);
        body.put("location", "Obra Villa El Salvador, Lima");
        body.put("status", "ACTIVE");
        body.put("autoRefill", true);
        body.put("refillThreshold", 25);
        body.put("lastRefillDate", "2026-10-08");
        return body;
    }

    private Long idOf(String name) {
        var id = idsByName.get(name);
        assertNotNull(id, "The equipment " + name + " was not registered in this scenario");
        return id;
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .defaultHeader("Authorization", "Bearer " + tokens.generateToken("buyer"))
                .build();
    }
}
