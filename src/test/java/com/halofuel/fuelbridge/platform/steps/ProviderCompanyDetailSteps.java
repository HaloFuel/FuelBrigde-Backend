package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.ProviderCompany;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.CreateProviderCompanyCommand;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.ProviderCompanyRepository;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CreateFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class ProviderCompanyDetailSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProviderCompanyRepository providerCompanyRepository;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    private static final AtomicLong NEXT_RUC = new AtomicLong(20701234567L);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProviderCompany provider;
    private MvcResult response;
    private Long targetProviderId;

    @Given("a provider company exists with name {string}")
    public void aProviderCompanyExistsWithName(String name) {
        var command = new CreateProviderCompanyCommand(
                name, String.valueOf(NEXT_RUC.getAndIncrement()), 4.5,
                "Av. Nestor Gambetta 5800, Callao",
                "+51976543210", List.of("DIESEL", "GASOLINE_95"),
                "Suministro de combustibles para obras y flotas."
        );
        provider = new ProviderCompany(command);
        provider = providerCompanyRepository.save(provider);
        assertNotNull(provider.getId());
        targetProviderId = provider.getId();
    }

    @And("the provider has {int} fuel order registered")
    public void theProviderHasFuelOrderRegistered(int count) {
        for (int i = 0; i < count; i++) {
            var command = new CreateFuelOrderCommand(
                    1L, provider.getId(), 1L, null,
                    300.0, "Av. Industrial 456, Lima",
                    LocalDate.now().plusDays(5)
            );
            var order = new FuelOrder(command, 900.0);
            fuelOrderRepository.save(order);
        }
    }

    @And("the provider has no fuel orders registered")
    public void theProviderHasNoFuelOrdersRegistered() {
        // no orders created — nothing to do
    }

    @Given("no provider company exists with id {long}")
    public void noProviderCompanyExistsWithId(Long id) {
        targetProviderId = id;
    }

    @When("the operator requests the detail of the provider")
    public void theOperatorRequestsTheDetailOfTheProvider() throws Exception {
        response = mockMvc.perform(
                get("/api/v1/provider-companies/" + targetProviderId + "/details")
                        .with(user("operator"))
        ).andReturn();
    }

    @When("the operator requests the detail of the provider with id {long}")
    public void theOperatorRequestsTheDetailOfTheProviderWithId(Long id) throws Exception {
        response = mockMvc.perform(
                get("/api/v1/provider-companies/" + id + "/details")
                        .with(user("operator"))
        ).andReturn();
    }

    @Then("the provider company response status code should be {int}")
    public void theProviderCompanyResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @And("the response should include the provider name {string}")
    public void theResponseShouldIncludeTheProviderName(String expectedName) throws Exception {
        String content = response.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        assertEquals(expectedName, json.get("name").asText());
    }

    @And("the response should include {int} order in the provider history")
    public void theResponseShouldIncludeOrderInTheProviderHistory(int expectedCount) throws Exception {
        String content = response.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        assertEquals(expectedCount, json.get("orders").size());
    }

    @And("the response should include {int} orders in the provider history")
    public void theResponseShouldIncludeOrdersInTheProviderHistory(int expectedCount) throws Exception {
        String content = response.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        assertEquals(expectedCount, json.get("orders").size());
    }
}