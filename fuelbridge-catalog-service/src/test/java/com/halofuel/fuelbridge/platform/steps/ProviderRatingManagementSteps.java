package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.catalog.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers listing, updating and validating provider ratings (TS-22). The IAM stub in
 * CucumberSpringConfiguration treats every company as existing except id 99.
 */
public class ProviderRatingManagementSteps {

    private static final ParameterizedTypeReference<Map<String, Object>> RATING =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> RATINGS =
            new ParameterizedTypeReference<>() { };

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    /** Rating ids created in this scenario, keyed by "company-provider". */
    private final Map<String, Long> ratingIds = new HashMap<>();

    private int lastStatus;
    private Map<String, Object> lastRating;
    private List<Map<String, Object>> lastRatings;

    @Given("buyer company {int} has rated provider {int} with {int}")
    public void buyerCompanyHasRatedProvider(int companyId, int providerId, int score) {
        submit(companyId, providerId, score);
        assertEquals(201, lastStatus, "The rating could not be created");
        ratingIds.put(key(companyId, providerId), ((Number) lastRating.get("id")).longValue());
    }

    @When("buyer company {int} submits a rating of {int} for provider {int}")
    public void buyerCompanySubmitsRating(int companyId, int score, int providerId) {
        submit(companyId, providerId, score);
    }

    @When("I list the ratings of provider {int}")
    public void iListTheRatingsOfProvider(int providerId) {
        list("providerId", providerId);
    }

    @When("I list the ratings given by buyer company {int}")
    public void iListTheRatingsGivenByBuyerCompany(int companyId) {
        list("companyId", companyId);
    }

    @When("buyer company {int} changes its rating of provider {int} to {int}")
    public void buyerCompanyChangesItsRating(int companyId, int providerId, int score) {
        update(idOf(companyId, providerId), companyId, providerId, score);
    }

    @When("buyer company {int} updates the rating with id {int} for provider {int} to {int}")
    public void buyerCompanyUpdatesRatingWithId(int companyId, int ratingId, int providerId, int score) {
        update((long) ratingId, companyId, providerId, score);
    }

    @When("buyer company {int} tries to move its rating of provider {int} to provider {int}")
    public void buyerCompanyTriesToMoveItsRating(int companyId, int providerId, int otherProviderId) {
        update(idOf(companyId, providerId), companyId, otherProviderId, 5);
    }

    @Then("the catalog responds with status {int}")
    public void theCatalogRespondsWithStatus(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus);
    }

    @Then("the returned rating is {int}")
    public void theReturnedRatingIs(int expectedScore) {
        assertNotNull(lastRating, "No rating was returned");
        assertEquals(expectedScore, ((Number) lastRating.get("rating")).intValue());
    }

    @Then("the listed ratings come from buyer companies {string}")
    public void theListedRatingsComeFromBuyerCompanies(String companyIds) {
        assertEquals(idsOf(companyIds), valuesOfLastList("companyId"));
    }

    @Then("the listed ratings are for providers {string}")
    public void theListedRatingsAreForProviders(String providerIds) {
        assertEquals(idsOf(providerIds), valuesOfLastList("providerId"));
    }

    @Then("no rating is stored for provider {int}")
    public void noRatingIsStoredForProvider(int providerId) {
        list("providerId", providerId);
        assertEquals(200, lastStatus);
        assertTrue(lastRatings.isEmpty(), "Expected no ratings for provider " + providerId);
    }

    private void submit(int companyId, int providerId, int score) {
        try {
            var response = client().post().uri("/api/v1/provider-ratings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body(companyId, providerId, score))
                    .retrieve().toEntity(RATING);
            lastStatus = response.getStatusCode().value();
            lastRating = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
            lastRating = null;
        }
    }

    private void update(Long ratingId, int companyId, int providerId, int score) {
        try {
            var response = client().put().uri("/api/v1/provider-ratings/{id}", ratingId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body(companyId, providerId, score))
                    .retrieve().toEntity(RATING);
            lastStatus = response.getStatusCode().value();
            lastRating = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
            lastRating = null;
        }
    }

    private void list(String filter, int id) {
        try {
            var response = client().get()
                    .uri(uri -> uri.path("/api/v1/provider-ratings").queryParam(filter, id).build())
                    .retrieve().toEntity(RATINGS);
            lastStatus = response.getStatusCode().value();
            lastRatings = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
            lastRatings = null;
        }
    }

    private static Map<String, Object> body(int companyId, int providerId, int score) {
        return Map.of("companyId", companyId, "providerId", providerId, "rating", score);
    }

    private Set<Long> valuesOfLastList(String field) {
        assertNotNull(lastRatings, "No list was returned");
        return lastRatings.stream()
                .map(rating -> ((Number) rating.get(field)).longValue())
                .collect(Collectors.toSet());
    }

    private static Set<Long> idsOf(String csv) {
        return Arrays.stream(csv.split(",")).map(String::trim).map(Long::valueOf).collect(Collectors.toSet());
    }

    private Long idOf(int companyId, int providerId) {
        var id = ratingIds.get(key(companyId, providerId));
        assertNotNull(id, "Buyer " + companyId + " has not rated provider " + providerId + " in this scenario");
        return id;
    }

    private static String key(int companyId, int providerId) {
        return companyId + "-" + providerId;
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .defaultHeader("Authorization", "Bearer " + tokens.generateToken("buyer"))
                .build();
    }
}
