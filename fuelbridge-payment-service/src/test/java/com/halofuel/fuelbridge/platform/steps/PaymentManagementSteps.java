package com.halofuel.fuelbridge.platform.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Covers payment registration and completion (TS-20) and payment queries (TS-21).
 * Completion talks to Ordering through {@link OrderingHttpStub}; payments are
 * referred to in the scenarios by the order they pay.
 */
public class PaymentManagementSteps {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    private final Map<Long, Long> paymentIdByOrder = new HashMap<>();

    private int lastStatus;
    private String lastBody;

    @Given("buyer company {int} has registered a payment of {double} for order {long}")
    public void buyerCompanyHasRegisteredAPayment(int companyId, double amount, long orderId) throws Exception {
        register(companyId, amount, "BANK_TRANSFER", orderId);
        assertEquals(201, lastStatus, "The payment could not be registered: " + lastBody);
    }

    @When("buyer company {int} registers a payment of {double} by {string} for order {long}")
    public void buyerCompanyRegistersAPayment(int companyId, double amount, String method, long orderId) throws Exception {
        register(companyId, amount, method, orderId);
    }

    @When("the payment of order {long} is completed with reference {string}")
    public void thePaymentOfOrderIsCompleted(long orderId, String reference) throws Exception {
        complete(idOf(orderId), reference);
    }

    @When("payment {long} is completed with reference {string}")
    public void paymentIsCompleted(long paymentId, String reference) throws Exception {
        complete(paymentId, reference);
    }

    @When("all payments are requested")
    public void allPaymentsAreRequested() throws Exception {
        perform(get("/api/v1/payments"));
    }

    @When("the payment of order {long} is requested by its id")
    public void thePaymentOfOrderIsRequestedByItsId(long orderId) throws Exception {
        perform(get("/api/v1/payments/{id}", idOf(orderId)));
    }

    @When("payment {long} is requested")
    public void paymentIsRequested(long paymentId) throws Exception {
        perform(get("/api/v1/payments/{id}", paymentId));
    }

    @When("the payment of order {long} is requested by order")
    public void thePaymentOfOrderIsRequestedByOrder(long orderId) throws Exception {
        perform(get("/api/v1/payments/order/{orderId}", orderId));
    }

    @When("the payments of company {int} are requested")
    public void thePaymentsOfCompanyAreRequested(int companyId) throws Exception {
        perform(get("/api/v1/payments/company/{companyId}", companyId));
    }

    @Then("the payment service responds with status {int}")
    public void thePaymentServiceRespondsWithStatus(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus, "Unexpected response: " + lastBody);
    }

    @Then("the returned payment is {string} for order {long} with an amount of {double}")
    public void theReturnedPaymentIs(String status, long orderId, double amount) {
        var payment = object();
        assertEquals(status, payment.get("status"));
        assertEquals(orderId, ((Number) payment.get("orderId")).longValue());
        assertEquals(amount, ((Number) payment.get("amount")).doubleValue());
    }

    @Then("the returned payment is {string} with reference {string}")
    public void theReturnedPaymentIsWithReference(String status, String reference) {
        var payment = object();
        assertEquals(status, payment.get("status"));
        assertEquals(reference, payment.get("transactionReference"));
        assertNotNull(payment.get("paidAt"), "A completed payment must record when it was paid");
    }

    @Then("the payment of order {long} is still {string}")
    public void thePaymentOfOrderIsStill(long orderId, String status) throws Exception {
        perform(get("/api/v1/payments/{id}", idOf(orderId)));
        assertEquals(200, lastStatus);
        assertEquals(status, object().get("status"));
    }

    @Then("Ordering was asked to mark order {long} as paid")
    public void orderingWasAskedToMarkOrderAsPaid(long orderId) {
        assertEquals(List.of(orderId), OrderingHttpStub.ordersMarkedAsPaid());
    }

    @Then("Ordering was not asked to mark any order as paid")
    public void orderingWasNotAskedToMarkAnyOrderAsPaid() {
        assertTrue(OrderingHttpStub.ordersMarkedAsPaid().isEmpty());
    }

    @Then("the listed payments belong to orders {string}")
    public void theListedPaymentsBelongToOrders(String orderIds) {
        Set<Long> expected = Arrays.stream(orderIds.split(","))
                .map(String::trim).map(Long::valueOf).collect(Collectors.toSet());
        List<?> payments = JSON.readValue(lastBody, List.class);
        Set<Long> actual = payments.stream()
                .map(payment -> ((Number) ((Map<?, ?>) payment).get("orderId")).longValue())
                .collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Then("the error code is {string}")
    public void theErrorCodeIs(String code) {
        assertEquals(code, object().get("code"));
    }

    private void register(int companyId, double amount, String method, long orderId) throws Exception {
        var body = """
                {"orderId":%d,"companyId":%d,"amount":%s,"paymentMethod":"%s"}"""
                .formatted(orderId, companyId, amount, method);
        perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(body));
        if (lastStatus == 201) {
            paymentIdByOrder.put(orderId, ((Number) object().get("id")).longValue());
        }
    }

    private void complete(long paymentId, String reference) throws Exception {
        perform(post("/api/v1/payments/{id}/complete", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transactionReference\":\"" + reference + "\"}"));
    }

    private void perform(MockHttpServletRequestBuilder request) throws Exception {
        var response = mockMvc.perform(request.with(user("operator"))).andReturn().getResponse();
        lastStatus = response.getStatus();
        lastBody = response.getContentAsString();
    }

    private Map<?, ?> object() {
        assertNotNull(lastBody, "The response had no body");
        assertTrue(!lastBody.isBlank(), "The response had no body");
        return JSON.readValue(lastBody, Map.class);
    }

    private Long idOf(long orderId) {
        var id = paymentIdByOrder.get(orderId);
        assertNotNull(id, "No payment for order " + orderId + " was registered in this scenario");
        return id;
    }
}
