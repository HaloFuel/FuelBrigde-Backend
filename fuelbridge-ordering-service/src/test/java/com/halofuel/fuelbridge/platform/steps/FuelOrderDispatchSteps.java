package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Steps for fuel-order-dispatch.feature (US-12): a provider dispatches a pending order. */
public class FuelOrderDispatchSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    @Autowired
    private BearerTokenService tokens;

    private FuelOrder fuelOrder;
    private MvcResult response;
    private String providerToken;

    @Given("a fuel order exists with status {string}")
    public void aFuelOrderExistsWithStatus(String status) {
        var order = new FuelOrder();
        order.setCompanyId(101L);
        order.setProviderId(202L);
        order.setFuelProductId(303L);
        order.setRequestedQuantity(300.0);
        order.setTotalPrice(1500.0);
        order.setDeliveryAddress("Test delivery address");
        order.setScheduledDate(LocalDate.now().plusDays(1));
        order.setStatus(OrderStatus.valueOf(status));
        fuelOrder = fuelOrderRepository.save(order);
        assertNotNull(fuelOrder.getId());
    }

    @Given("the provider is authenticated")
    public void theProviderIsAuthenticated() {
        // JWT issued by Identity & Access and validated locally with the shared secret.
        providerToken = tokens.generateToken("provider");
    }

    @When("the provider sends a POST request to {string}")
    public void theProviderSendsPostRequest(String endpoint) throws Exception {
        assertNotNull(providerToken);
        var url = endpoint.replace("{orderId}", fuelOrder.getId().toString());
        response = mockMvc.perform(post(url).header("Authorization", "Bearer " + providerToken)).andReturn();
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the fuel order status should be {string}")
    public void theFuelOrderStatusShouldBe(String expectedStatus) {
        var updatedOrder = fuelOrderRepository.findById(fuelOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.valueOf(expectedStatus), updatedOrder.getStatus());
    }
}
