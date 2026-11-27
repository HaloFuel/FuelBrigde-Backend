package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.RequestStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.FuelRequestPersistenceEntity;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.FuelRequestPersistenceRepository;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Steps for the Ordering REST API seen by the frontend: fuel order creation (TS-04),
 * order queries (TS-05, TS-13) and the fuel request flow (TS-15). Every request carries
 * a JWT validated locally with the secret shared with Identity &amp; Access.
 */
public class OrderingApiSteps {

    private static final String ADDRESS = "Av. Separadora Industrial 2450, Villa El Salvador, Lima";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuelOrderRepository orders;

    @Autowired
    private FuelRequestPersistenceRepository requests;

    @Autowired
    private InventoryHttpStub inventoryService;

    @Autowired
    private BearerTokenService tokens;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MvcResult response;
    private Long firstOrderId;
    private Long fuelRequestId;

    // ------------------------------------------------------------------ fuel orders (TS-04)

    @Given("the fuel product {long} of provider {long} costs {double} per unit")
    public void theFuelProductCosts(long productId, long providerId, double price) {
        inventoryService.addProduct(productId, providerId, price);
    }

    @When("a buyer creates a fuel order for product {long} of provider {long} with {double} units")
    public void aBuyerCreatesAFuelOrder(long productId, long providerId, double quantity) throws Exception {
        send(post("/api/v1/fuel-orders"), orderBody(providerId, productId, quantity, ADDRESS));
    }

    @When("a buyer creates a fuel order for product {long} of provider {long} without the delivery address")
    public void aBuyerCreatesAFuelOrderWithoutAddress(long productId, long providerId) throws Exception {
        send(post("/api/v1/fuel-orders"), orderBody(providerId, productId, 1000.0, null));
    }

    @Then("the ordering response status should be {int}")
    public void theOrderingResponseStatusShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the response contains the new order id with status {string} and total price {double}")
    public void theResponseContainsTheNewOrder(String status, double totalPrice) throws Exception {
        var body = body();
        assertTrue(body.get("id").asLong() > 0);
        assertEquals(status, body.get("status").asText());
        assertEquals(totalPrice, body.get("totalPrice").asDouble(), 0.001);
        assertTrue(orders.findById(body.get("id").asLong()).isPresent());
    }

    @Then("the ordering error code should be {string}")
    public void theOrderingErrorCodeShouldBe(String code) throws Exception {
        assertEquals(code, body().get("code").asText());
    }

    @Then("Inventory was called with the ordering service token")
    public void inventoryWasCalledWithTheServiceToken() {
        var authorization = inventoryService.lastAuthorization();
        assertNotNull(authorization);
        var jwt = authorization.substring("Bearer ".length());
        assertTrue(tokens.validateToken(jwt));
        assertEquals("fuelbridge-ordering-service", tokens.getUsernameFromToken(jwt));
    }

    @Then("no fuel order is stored")
    public void noFuelOrderIsStored() {
        assertTrue(orders.findAll().isEmpty());
    }

    // ------------------------------------------------------------------ order queries (TS-05, TS-13)

    @Given("company {long} has {int} fuel orders with provider {long}")
    public void companyHasFuelOrders(long companyId, int count, long providerId) {
        for (int i = 0; i < count; i++) {
            var order = new FuelOrder();
            order.setCompanyId(companyId);
            order.setProviderId(providerId);
            order.setFuelProductId(303L);
            order.setRequestedQuantity(1000.0 + i);
            order.setTotalPrice(4800.0);
            order.setDeliveryAddress(ADDRESS);
            order.setScheduledDate(LocalDate.now().plusDays(2));
            order.setStatus(OrderStatus.PENDING);
            var saved = orders.save(order);
            if (firstOrderId == null) {
                firstOrderId = saved.getId();
            }
        }
    }

    @When("all fuel orders are requested")
    public void allFuelOrdersAreRequested() throws Exception {
        send(get("/api/v1/fuel-orders"), null);
    }

    @When("the orders of company {long} are requested")
    public void theOrdersOfCompanyAreRequested(long companyId) throws Exception {
        send(get("/api/v1/fuel-orders/company/{companyId}", companyId), null);
    }

    @When("the orders of provider {long} are requested")
    public void theOrdersOfProviderAreRequested(long providerId) throws Exception {
        send(get("/api/v1/fuel-orders/provider/{providerId}", providerId), null);
    }

    @When("the first stored order is requested by its identifier")
    public void theFirstStoredOrderIsRequested() throws Exception {
        send(get("/api/v1/fuel-orders/{orderId}", firstOrderId), null);
    }

    @When("the fuel order {long} is requested")
    public void theFuelOrderIsRequested(long orderId) throws Exception {
        send(get("/api/v1/fuel-orders/{orderId}", orderId), null);
    }

    @Then("the response lists {int} fuel orders")
    public void theResponseListsFuelOrders(int count) throws Exception {
        var body = body();
        assertTrue(body.isArray());
        assertEquals(count, body.size());
    }

    @Then("every listed order belongs to company {long}")
    public void everyListedOrderBelongsToCompany(long companyId) throws Exception {
        body().forEach(order -> assertEquals(companyId, order.get("companyId").asLong()));
    }

    @Then("every listed order belongs to provider {long}")
    public void everyListedOrderBelongsToProvider(long providerId) throws Exception {
        body().forEach(order -> assertEquals(providerId, order.get("providerId").asLong()));
    }

    @Then("the response shows that order with status {string}")
    public void theResponseShowsThatOrder(String status) throws Exception {
        var body = body();
        assertEquals(firstOrderId.longValue(), body.get("id").asLong());
        assertEquals(status, body.get("status").asText());
    }

    // ------------------------------------------------------------------ fuel requests (TS-15)

    @When("a buyer creates a fuel request for {double} units of product {long} of provider {long}")
    public void aBuyerCreatesAFuelRequest(double quantity, long productId, long providerId) throws Exception {
        send(post("/api/v1/fuel-requests"), requestBody(providerId, productId, quantity, LocalDate.now().plusDays(3)));
    }

    @When("a buyer creates a fuel request for product {long} of provider {long} without a delivery date")
    public void aBuyerCreatesAFuelRequestWithoutDate(long productId, long providerId) throws Exception {
        send(post("/api/v1/fuel-requests"), requestBody(providerId, productId, 1000.0, null));
    }

    @Then("the response contains the new fuel request id with status {string}")
    public void theResponseContainsTheNewFuelRequest(String status) throws Exception {
        var body = body();
        assertTrue(body.get("id").asLong() > 0);
        assertEquals(status, body.get("status").asText());
        assertEquals("Diesel B5 S-50", body.get("productName").asText());
    }

    @Given("a {word} fuel request from buyer {long} for {double} units of product {long} of provider {long}")
    public void aFuelRequestFromBuyer(String status, long buyerCompanyId, double quantity, long productId,
                                      long providerId) {
        var entity = new FuelRequestPersistenceEntity();
        entity.setBuyerCompanyId(buyerCompanyId);
        entity.setProviderId(providerId);
        entity.setFuelProductId(productId);
        entity.setFuelType("DIESEL");
        entity.setProductName("Diesel B5 S-50");
        entity.setQuantity(quantity);
        entity.setUnit("L");
        entity.setUnitPrice(4.8);
        entity.setDeliveryAddress(ADDRESS);
        entity.setDeliveryDate(LocalDate.now().plusDays(3));
        entity.setStatus(RequestStatus.valueOf(status.toUpperCase()));
        entity.setSource("MANUAL");
        fuelRequestId = requests.save(entity).getId();
    }

    @When("the fuel requests of buyer {long} are listed")
    public void theFuelRequestsOfBuyerAreListed(long buyerCompanyId) throws Exception {
        send(get("/api/v1/fuel-requests").param("buyerCompanyId", String.valueOf(buyerCompanyId)), null);
    }

    @Then("the response lists {int} fuel requests")
    public void theResponseListsFuelRequests(int count) throws Exception {
        var body = body();
        assertTrue(body.isArray());
        assertEquals(count, body.size());
    }

    @When("the provider accepts the fuel request")
    public void theProviderAcceptsTheFuelRequest() throws Exception {
        send(post("/api/v1/fuel-requests/{requestId}/accept", fuelRequestId), null);
    }

    @When("the provider rejects the fuel request with reason {string}")
    public void theProviderRejectsTheFuelRequest(String reason) throws Exception {
        send(post("/api/v1/fuel-requests/{requestId}/reject", fuelRequestId), Map.of("reason", reason));
    }

    @Then("the fuel request status should be {string}")
    public void theFuelRequestStatusShouldBe(String status) {
        var stored = requests.findById(fuelRequestId).orElseThrow();
        assertEquals(RequestStatus.valueOf(status), stored.getStatus());
    }

    @Then("the response is a {string} order created from the fuel request with total price {double}")
    public void theResponseIsAnOrderCreatedFromTheRequest(String status, double totalPrice) throws Exception {
        var body = body();
        assertEquals(fuelRequestId.longValue(), body.get("requestId").asLong());
        assertEquals(status, body.get("status").asText());
        assertEquals(totalPrice, body.get("totalPrice").asDouble(), 0.001);
        assertTrue(orders.findById(body.get("id").asLong()).isPresent());
    }

    @Then("the fuel request keeps the rejection reason {string}")
    public void theFuelRequestKeepsTheRejectionReason(String reason) {
        var stored = requests.findById(fuelRequestId).orElseThrow();
        assertEquals(reason, stored.getRejectionReason());
    }

    // ------------------------------------------------------------------ helpers

    private void send(MockHttpServletRequestBuilder request, Object body) throws Exception {
        request.header("Authorization", "Bearer " + tokens.generateToken("buyer"));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        response = mockMvc.perform(request).andReturn();
    }

    private JsonNode body() throws Exception {
        return objectMapper.readTree(response.getResponse().getContentAsString());
    }

    private static Map<String, Object> orderBody(long providerId, long productId, double quantity, String address) {
        var body = new LinkedHashMap<String, Object>();
        body.put("companyId", 101);
        body.put("providerId", providerId);
        body.put("fuelProductId", productId);
        body.put("equipmentId", 305);
        body.put("requestedQuantity", quantity);
        body.put("deliveryAddress", address);
        body.put("scheduledDate", LocalDate.now().plusDays(2).toString());
        return body;
    }

    private static Map<String, Object> requestBody(long providerId, long productId, double quantity,
                                                   LocalDate deliveryDate) {
        var body = new LinkedHashMap<String, Object>();
        body.put("buyerCompanyId", 101);
        body.put("providerId", providerId);
        body.put("equipmentId", 305);
        body.put("fuelProductId", productId);
        body.put("quantity", quantity);
        body.put("unit", "L");
        body.put("deliveryAddress", ADDRESS);
        body.put("deliveryDate", deliveryDate == null ? null : deliveryDate.toString());
        body.put("source", "manual");
        return body;
    }
}
