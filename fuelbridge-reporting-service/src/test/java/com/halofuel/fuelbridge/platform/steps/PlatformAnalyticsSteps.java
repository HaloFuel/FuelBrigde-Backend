package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.reporting.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlatformAnalyticsSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    private HttpStatusCode lastStatus;
    private Map<?, ?> lastBody;

    @Given("buyer company {int} has placed orders with completed and pending payments")
    public void buyerCompanyHasPlacedOrdersWithCompletedAndPendingPayments(int companyId) {
        // AnalyticsSourceStub already serves one CONFIRMED/COMPLETED order (100)
        // and one PENDING/PENDING order (30) for company 7; nothing else to set up.
    }

    @Given("the upstream ordering service is unavailable")
    public void theUpstreamOrderingServiceIsUnavailable() {
        AnalyticsSourceStub.UNAVAILABLE.set(true);
    }

    @When("a stakeholder requests analytics for buyer company {int}")
    public void aStakeholderRequestsAnalyticsForBuyerCompany(int companyId) {
        request("/api/v1/analytics/buyers/" + companyId);
    }

    @When("a stakeholder requests the platform summary")
    public void aStakeholderRequestsThePlatformSummary() {
        request("/api/v1/analytics/platform");
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus.value());
    }

    @Then("the total spent should be {double}")
    public void theTotalSpentShouldBe(double expectedTotal) {
        assertEquals(expectedTotal, ((Number) lastBody.get("totalSpent")).doubleValue());
    }

    @Then("there should be {int} completed payment and {int} pending payment")
    public void thereShouldBeCompletedAndPendingPayments(int completed, int pending) {
        assertEquals(completed, ((Number) lastBody.get("completedPayments")).intValue());
        assertEquals(pending, ((Number) lastBody.get("pendingPayments")).intValue());
    }

    private void request(String uri) {
        try {
            var response = client().get().uri(uri).retrieve().toEntity(Map.class);
            lastStatus = response.getStatusCode();
            lastBody = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode();
            lastBody = null;
        }
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .defaultHeader("Authorization", "Bearer " + tokens.generateToken("buyer"))
                .build();
    }
}
