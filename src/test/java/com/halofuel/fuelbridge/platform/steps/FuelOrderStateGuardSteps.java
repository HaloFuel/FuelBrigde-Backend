package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Steps for fuel-order-state-guards.feature: transition guards of the fuel order aggregate seen
 * through the REST API (409 for an invalid transition, 404 for an unknown order).
 */
public class FuelOrderStateGuardSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FuelOrder existingOrder;
    private MvcResult response;

    @Given("an existing fuel order in status {string}")
    public void anExistingFuelOrderInStatus(String status) {
        var order = new FuelOrder();
        order.setCompanyId(101L);
        order.setProviderId(202L);
        order.setFuelProductId(303L);
        order.setRequestedQuantity(500.0);
        order.setTotalPrice(2400.0);
        order.setDeliveryAddress("Av. Separadora Industrial 2450, Villa El Salvador, Lima");
        order.setScheduledDate(LocalDate.now().plusDays(1));
        order.setStatus(OrderStatus.valueOf(status));
        existingOrder = fuelOrderRepository.save(order);
        assertNotNull(existingOrder.getId());
    }

    @When("a user cancels the existing order")
    public void aUserCancelsTheExistingOrder() throws Exception {
        transition("cancel", existingOrder.getId());
    }

    @When("a user cancels order {long}")
    public void aUserCancelsOrder(Long orderId) throws Exception {
        transition("cancel", orderId);
    }

    @When("a user confirms the existing order")
    public void aUserConfirmsTheExistingOrder() throws Exception {
        transition("confirm", existingOrder.getId());
    }

    @When("a service marks the existing order as paid")
    public void aServiceMarksTheExistingOrderAsPaid() throws Exception {
        transition("mark-paid", existingOrder.getId());
    }

    @Then("the guard response status code should be {int}")
    public void theGuardResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the guard error should say {string}")
    public void theGuardErrorShouldSay(String expectedDetails) throws Exception {
        var body = objectMapper.readTree(response.getResponse().getContentAsString());
        assertEquals("FUELORDER_CONFLICT", body.get("code").asText());
        assertEquals(expectedDetails, body.get("details").asText());
    }

    @Then("the existing order status should be {string}")
    public void theExistingOrderStatusShouldBe(String expectedStatus) {
        var stored = fuelOrderRepository.findById(existingOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.valueOf(expectedStatus), stored.getStatus());
    }

    private void transition(String name, Long orderId) throws Exception {
        response = mockMvc.perform(post("/api/v1/fuel-orders/%d/%s".formatted(orderId, name))
                .with(user("buyer"))).andReturn();
    }
}
