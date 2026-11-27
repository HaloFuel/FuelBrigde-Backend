package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.BuyerCompany;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.CreateBuyerCompanyCommand;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.BuyerCompanyRepository;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class BuyerCompanyListSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuyerCompanyRepository buyerCompanyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // static: Cucumber creates a new glue instance per scenario, and ruc is unique in buyer_companies.
    // Range starts below the one used by BuyerCompanyDetailSteps (20609876543 upwards) so they never overlap.
    private static final AtomicLong NEXT_RUC = new AtomicLong(20100000000L);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MvcResult response;

    @Given("a registered buyer company named {string}")
    public void aRegisteredBuyerCompanyNamed(String name) {
        var command = new CreateBuyerCompanyCommand(
                name, String.valueOf(NEXT_RUC.getAndIncrement()), "LOGISTICS",
                "Av. Argentina 2100, Lima",
                "compras@empresa.example", "+51912345678"
        );
        var company = buyerCompanyRepository.save(new BuyerCompany(command));
        assertNotNull(company.getId());
    }

    @Given("there are no buyer companies registered")
    public void thereAreNoBuyerCompaniesRegistered() {
        jdbcTemplate.update("DELETE FROM buyer_companies");
        assertTrue(buyerCompanyRepository.findAll().isEmpty());
    }

    @When("the operator requests the list of buyer companies")
    public void theOperatorRequestsTheListOfBuyerCompanies() throws Exception {
        response = mockMvc.perform(
                get("/api/v1/buyer-companies")
                        .with(user("operator"))
        ).andReturn();
    }

    @Then("the buyer company list response status code should be {int}")
    public void theBuyerCompanyListResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @And("the buyer company list should contain a company named {string}")
    public void theBuyerCompanyListShouldContainACompanyNamed(String expectedName) throws Exception {
        JsonNode json = objectMapper.readTree(response.getResponse().getContentAsString());
        assertTrue(json.isArray());
        boolean found = StreamSupport.stream(json.spliterator(), false)
                .anyMatch(company -> expectedName.equals(company.get("name").asText()));
        assertTrue(found, "Expected buyer company list to contain \"" + expectedName + "\" but was: " + json);
    }

    @And("the buyer company list should be empty")
    public void theBuyerCompanyListShouldBeEmpty() throws Exception {
        JsonNode json = objectMapper.readTree(response.getResponse().getContentAsString());
        assertTrue(json.isArray());
        assertEquals(0, json.size());
    }
}
