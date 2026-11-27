package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.catalog.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the catalog service only accepts requests carrying a valid JWT
 * signed with the secret shared with Identity &amp; Access (T09.12).
 */
public class SecuritySteps {

    private static final String PROTECTED_ENDPOINT = "/api/v1/provider-ratings";

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    private int lastStatus;

    @When("the provider ratings are requested without a token")
    public void requestedWithoutToken() {
        send(null);
    }

    @When("the provider ratings are requested with a tampered token")
    public void requestedWithTamperedToken() {
        send(tamper(tokens.generateToken("buyer")));
    }

    @When("the provider ratings are requested with the token {string}")
    public void requestedWithToken(String token) {
        send(token);
    }

    @Then("the request is rejected with status {int}")
    public void theRequestIsRejectedWithStatus(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus);
    }

    private void send(String token) {
        var request = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build()
                .get().uri(PROTECTED_ENDPOINT);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        try {
            lastStatus = request.retrieve().toBodilessEntity().getStatusCode().value();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
        }
    }

    /**
     * Changes the first character of the signature so the token no longer verifies.
     * The last character is avoided because its low bits can be Base64 padding.
     */
    private static String tamper(String jwt) {
        int signatureStart = jwt.lastIndexOf('.') + 1;
        char first = jwt.charAt(signatureStart);
        return jwt.substring(0, signatureStart) + (first == 'A' ? 'B' : 'A') + jwt.substring(signatureStart + 1);
    }
}
