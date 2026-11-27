package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.RequestStatus;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.FuelRequestPersistenceEntity;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.FuelRequestPersistenceRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class FuelRequestDetailSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuelRequestPersistenceRepository fuelRequestRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FuelRequestPersistenceEntity request;
    private MvcResult response;

    @Given("a pending fuel request from buyer {long} to provider {long} for {double} liters")
    public void aPendingFuelRequest(long buyerCompanyId, long providerId, double quantity) {
        var entity = new FuelRequestPersistenceEntity();
        entity.setBuyerCompanyId(buyerCompanyId);
        entity.setProviderId(providerId);
        entity.setFuelProductId(303L);
        entity.setFuelType("DIESEL");
        entity.setProductName("Diesel B5 S-50");
        entity.setQuantity(quantity);
        entity.setUnit("L");
        entity.setUnitPrice(4.8);
        entity.setDeliveryAddress("Av. Separadora Industrial 2450, Villa El Salvador, Lima");
        entity.setDeliveryDate(LocalDate.now().plusDays(3));
        entity.setStatus(RequestStatus.PENDING);
        entity.setSource("MANUAL");
        request = fuelRequestRepository.save(entity);
    }

    @When("the user requests that fuel request by its identifier")
    public void theUserRequestsThatFuelRequest() throws Exception {
        response = fetch(request.getId());
    }

    @When("the user requests the fuel request with id {long}")
    public void theUserRequestsTheFuelRequestWithId(long requestId) throws Exception {
        response = fetch(requestId);
    }

    @Then("the fuel request detail response status should be {int}")
    public void theFuelRequestDetailResponseStatusShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the fuel request detail should show status {string} and {double} liters")
    public void theFuelRequestDetailShouldShow(String expectedStatus, double expectedQuantity) throws Exception {
        var body = objectMapper.readTree(response.getResponse().getContentAsString());
        assertEquals(request.getId().longValue(), body.get("id").asLong());
        assertEquals(expectedStatus, body.get("status").asText());
        assertEquals(expectedQuantity, body.get("quantity").asDouble());
    }

    private MvcResult fetch(long requestId) throws Exception {
        return mockMvc.perform(get("/api/v1/fuel-requests/" + requestId).with(user("buyer"))).andReturn();
    }
}
