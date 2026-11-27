package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Steps for fuel-order-state-transitions.feature: the transitions that Payment and
 * Fulfillment request through REST. The calls carry a service token, as a standalone
 * service would send them.
 */
public class FuelOrderStateTransitionSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    @Autowired
    private ServiceTokenIssuer serviceTokens;

    private FuelOrder storedOrder;
    private MvcResult response;

    @Given("a stored fuel order with status {string}")
    public void aStoredFuelOrderWithStatus(String status) {
        var order = new FuelOrder();
        order.setCompanyId(101L);
        order.setProviderId(202L);
        order.setFuelProductId(303L);
        order.setRequestedQuantity(500.0);
        order.setTotalPrice(2400.0);
        order.setDeliveryAddress("Av. Separadora Industrial 2450, Villa El Salvador, Lima");
        order.setScheduledDate(LocalDate.now().plusDays(1));
        order.setStatus(OrderStatus.valueOf(status));
        storedOrder = fuelOrderRepository.save(order);
        assertNotNull(storedOrder.getId());
    }

    @When("a service requests the {string} transition for the stored order")
    public void aServiceRequestsTheTransitionForTheStoredOrder(String transition) throws Exception {
        requestTransition(transition, storedOrder.getId());
    }

    @When("a service requests the {string} transition for order {long}")
    public void aServiceRequestsTheTransitionForOrder(String transition, Long orderId) throws Exception {
        requestTransition(transition, orderId);
    }

    @Then("the transition response status code should be {int}")
    public void theTransitionResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the stored fuel order status should be {string}")
    public void theStoredFuelOrderStatusShouldBe(String expectedStatus) {
        var updated = fuelOrderRepository.findById(storedOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.valueOf(expectedStatus), updated.getStatus());
    }

    private void requestTransition(String transition, Long orderId) throws Exception {
        var url = "/api/v1/fuel-orders/%d/%s".formatted(orderId, transition);
        var token = serviceTokens.issueFor("fuelbridge-fulfillment-service");
        response = mockMvc.perform(post(url).header("Authorization", "Bearer " + token)).andReturn();
    }
}
