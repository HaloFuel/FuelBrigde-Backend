package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.infrastructure.security.tokens.jwt.BearerTokenService;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Verifies that the ordering service only accepts requests carrying a valid JWT signed
 * with the secret shared with Identity &amp; Access: a user token or the short-lived
 * service token of another FuelBridge service.
 */
public class SecuritySteps {

    private static final String PROTECTED_ENDPOINT = "/api/v1/fuel-orders";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BearerTokenService tokens;

    @Autowired
    private ServiceTokenIssuer serviceTokens;

    private int lastStatus;

    @When("the fuel orders are requested without a token")
    public void requestedWithoutToken() throws Exception {
        send(null);
    }

    @When("the fuel orders are requested with a tampered token")
    public void requestedWithTamperedToken() throws Exception {
        send(tamper(tokens.generateToken("buyer")));
    }

    @When("the fuel orders are requested with the token {string}")
    public void requestedWithToken(String token) throws Exception {
        send(token);
    }

    @When("the fuel orders are requested with a user token")
    public void requestedWithUserToken() throws Exception {
        send(tokens.generateToken("buyer"));
    }

    @When("the fuel orders are requested with the service token of {string}")
    public void requestedWithServiceToken(String serviceName) throws Exception {
        send(serviceTokens.issueFor(serviceName));
    }

    @Then("the ordering request is answered with status {int}")
    public void theRequestIsAnsweredWithStatus(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus);
    }

    private void send(String token) throws Exception {
        var request = get(PROTECTED_ENDPOINT);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        lastStatus = mockMvc.perform(request).andReturn().getResponse().getStatus();
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
