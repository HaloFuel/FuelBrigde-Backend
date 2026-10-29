package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.catalog.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProviderRatingSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    private Long companyId;
    private Long providerId;
    private String jwt;
    private HttpStatusCode lastStatus;
    private Map<?, ?> lastBody;

    @Given("buyer company {int} and provider company {int} are known to IAM")
    public void companiesAreKnownToIam(int companyId, int providerId) {
        this.companyId = (long) companyId;
        this.providerId = (long) providerId;
    }

    @Given("the buyer is authenticated")
    public void theBuyerIsAuthenticated() {
        this.jwt = tokens.generateToken("buyer");
    }

    @Given("the buyer has already rated the provider with {int}")
    public void theBuyerHasAlreadyRatedTheProviderWith(int rating) {
        rateProvider(rating);
        assertEquals(201, lastStatus.value());
    }

    @When("the buyer rates the provider with {int}")
    public void theBuyerRatesTheProviderWith(int rating) {
        rateProvider(rating);
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus.value());
    }

    @Then("the stored rating should be {int}")
    public void theStoredRatingShouldBe(int expectedRating) {
        assertEquals(expectedRating, ((Number) lastBody.get("rating")).intValue());
    }

    private void rateProvider(int rating) {
        try {
            var response = client().post().uri("/api/v1/provider-ratings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("companyId", companyId, "providerId", providerId, "rating", rating))
                    .retrieve().toEntity(Map.class);
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
                .defaultHeader("Authorization", "Bearer " + jwt)
                .build();
    }
}
