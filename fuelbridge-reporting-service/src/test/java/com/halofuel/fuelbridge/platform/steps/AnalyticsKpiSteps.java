package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.reporting.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the platform summary, provider analytics and the no-data case (TS-27).
 * Expected values come from the fixtures documented in {@link AnalyticsSourceStub}.
 */
public class AnalyticsKpiSteps {

    private static final ParameterizedTypeReference<Map<String, Object>> KPIS =
            new ParameterizedTypeReference<>() { };

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    private int lastStatus;
    private Map<String, Object> lastKpis;

    @Given("the platform has registered orders, deliveries and payments")
    public void thePlatformHasRegisteredOrdersDeliveriesAndPayments() {
        // AnalyticsSourceStub serves the platform-wide fixtures; nothing else to set up.
    }

    @When("a stakeholder asks for the platform KPIs")
    public void aStakeholderAsksForThePlatformKpis() {
        request("/api/v1/analytics/platform");
    }

    @When("a stakeholder asks for the KPIs of provider {int}")
    public void aStakeholderAsksForTheKpisOfProvider(int providerId) {
        request("/api/v1/analytics/providers/" + providerId);
    }

    @When("a stakeholder asks for the KPIs of buyer company {int}")
    public void aStakeholderAsksForTheKpisOfBuyerCompany(int companyId) {
        request("/api/v1/analytics/buyers/" + companyId);
    }

    @Then("the analytics respond with status {int}")
    public void theAnalyticsRespondWithStatus(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus);
    }

    @Then("the platform shows {int} orders of which {int} is pending")
    public void thePlatformShowsOrders(int totalOrders, int pendingOrders) {
        assertEquals(totalOrders, number("totalOrders").intValue());
        assertEquals(pendingOrders, number("pendingOrders").intValue());
    }

    @Then("the platform shows {int} deliveries of which {int} is completed")
    public void thePlatformShowsDeliveries(int totalDeliveries, int completedDeliveries) {
        assertEquals(totalDeliveries, number("totalDeliveries").intValue());
        assertEquals(completedDeliveries, number("completedDeliveries").intValue());
    }

    @Then("the platform shows {int} payments with a revenue of {double}")
    public void thePlatformShowsPayments(int totalPayments, double totalRevenue) {
        assertEquals(totalPayments, number("totalPayments").intValue());
        assertEquals(totalRevenue, number("totalRevenue").doubleValue());
    }

    @Then("the provider has {int} orders, {int} confirmed and {int} cancelled")
    public void theProviderHasOrders(int totalOrders, int confirmedOrders, int cancelledOrders) {
        assertEquals(totalOrders, number("totalOrders").intValue());
        assertEquals(confirmedOrders, number("confirmedOrders").intValue());
        assertEquals(cancelledOrders, number("cancelledOrders").intValue());
    }

    @Then("the provider revenue is {double}")
    public void theProviderRevenueIs(double revenue) {
        assertEquals(revenue, number("totalRevenue").doubleValue());
    }

    @Then("the provider revenue is split into {int} months")
    public void theProviderRevenueIsSplitIntoMonths(int months) {
        assertEquals(months, ((List<?>) field("monthlyRevenue")).size());
    }

    @Then("the buyer has {int} orders and spent {double}")
    public void theBuyerHasOrdersAndSpent(int totalOrders, double totalSpent) {
        assertEquals(totalOrders, number("totalOrders").intValue());
        assertEquals(totalSpent, number("totalSpent").doubleValue());
    }

    private void request(String uri) {
        try {
            var response = client().get().uri(uri).retrieve().toEntity(KPIS);
            lastStatus = response.getStatusCode().value();
            lastKpis = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
            lastKpis = null;
        }
    }

    private Number number(String name) {
        return (Number) field(name);
    }

    private Object field(String name) {
        assertNotNull(lastKpis, "No analytics were returned");
        var value = lastKpis.get(name);
        assertNotNull(value, "The response has no field " + name);
        return value;
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .defaultHeader("Authorization", "Bearer " + tokens.generateToken("buyer"))
                .build();
    }
}
