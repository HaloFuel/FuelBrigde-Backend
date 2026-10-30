package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.BuyerCompany;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.CreateBuyerCompanyCommand;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.BuyerCompanyRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class BuyerCompanyDetailSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuyerCompanyRepository buyerCompanyRepository;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BuyerCompany company;
    private MvcResult response;
    private Long targetCompanyId;

    @Given("a buyer company exists with name {string}")
    public void aBuyerCompanyExistsWithName(String name) {
        var command = new CreateBuyerCompanyCommand(
                name, "20609876543", "CONSTRUCTION",
                "Av. Industrial 123, Lima",
                "contacto@empresa.example", "+51987654321"
        );
        company = new BuyerCompany(command);
        company = buyerCompanyRepository.save(company);
        assertNotNull(company.getId());
        targetCompanyId = company.getId();
    }

    @And("the company has {int} fuel order registered")
    public void theCompanyHasFuelOrderRegistered(int count) {
        for (int i = 0; i < count; i++) {
            var command = new CreateFuelOrderCommand(
                    company.getId(), 1L, 1L, null,
                    500.0, "Av. Entrega 456, Lima",
                    LocalDate.now().plusDays(3)
            );
            var order = new FuelOrder(command, 1500.0);
            fuelOrderRepository.save(order);
        }
    }

    @And("the company has no fuel orders registered")
    public void theCompanyHasNoFuelOrdersRegistered() {
        // no orders created — nothing to do
    }

    @Given("no buyer company exists with id {long}")
    public void noBuyerCompanyExistsWithId(Long id) {
        targetCompanyId = id;
    }

    @When("the operator requests the detail of the company")
    public void theOperatorRequestsTheDetailOfTheCompany() throws Exception {
        response = mockMvc.perform(
                get("/api/v1/buyer-companies/" + targetCompanyId + "/details")
                        .with(user("operator"))
        ).andReturn();
    }

    @When("the operator requests the detail of the company with id {long}")
    public void theOperatorRequestsTheDetailOfTheCompanyWithId(Long id) throws Exception {
        response = mockMvc.perform(
                get("/api/v1/buyer-companies/" + id + "/details")
                        .with(user("operator"))
        ).andReturn();
    }

    @Then("the buyer company response status code should be {int}")
    public void theBuyerCompanyResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @And("the response should include the company name {string}")
    public void theResponseShouldIncludeTheCompanyName(String expectedName) throws Exception {
        String content = response.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        assertEquals(expectedName, json.get("name").asText());
    }

    @And("the response should include {int} order in the history")
    public void theResponseShouldIncludeOrderInTheHistory(int expectedCount) throws Exception {
        String content = response.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        assertEquals(expectedCount, json.get("orders").size());
    }

    @And("the response should include {int} orders in the history")
    public void theResponseShouldIncludeOrdersInTheHistory(int expectedCount) throws Exception {
        String content = response.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        assertEquals(expectedCount, json.get("orders").size());
    }
}