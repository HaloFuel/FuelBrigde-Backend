package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.RevokedToken;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.RevokedTokenRepository;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;
import com.halofuel.fuelbridge.platform.iam.infrastructure.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Steps for user-sign-out.feature (TS-03). No authentication post-processor is used: the
 * requests carry a real signed JWT so the Bearer filter and the blacklist are exercised.
 */
public class UserSignOutSteps {

    private static final String SIGN_OUT_URL = "/api/v1/authentication/sign-out";
    private static final String PROTECTED_URL = "/api/v1/users";

    // static: Cucumber creates a new glue instance per scenario and username is unique in users.
    private static final AtomicLong NEXT_SUFFIX = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Autowired
    private BearerTokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;
    private String otherToken;
    private MvcResult response;

    @Given("a registered user with a valid token")
    public void aRegisteredUserWithAValidToken() {
        token = registerUserAndIssueToken();
    }

    @Given("another registered user with a valid token")
    public void anotherRegisteredUserWithAValidToken() {
        otherToken = registerUserAndIssueToken();
    }

    @Given("the token is accepted by a protected endpoint")
    public void theTokenIsAcceptedByAProtectedEndpoint() throws Exception {
        assertEquals(200, requestProtectedEndpoint(token).getResponse().getStatus());
    }

    @Given("the user has signed out with that token")
    public void theUserHasSignedOutWithThatToken() throws Exception {
        assertEquals(200, signOut("Bearer " + token).getResponse().getStatus());
    }

    @When("the user signs out with that token")
    public void theUserSignsOutWithThatToken() throws Exception {
        response = signOut("Bearer " + token);
    }

    @When("a client signs out without a token")
    public void aClientSignsOutWithoutAToken() throws Exception {
        response = mockMvc.perform(post(SIGN_OUT_URL)).andReturn();
    }

    @When("the user requests a protected endpoint with that token")
    public void theUserRequestsAProtectedEndpointWithThatToken() throws Exception {
        response = requestProtectedEndpoint(token);
    }

    @Then("the sign-out response status code should be {int}")
    public void theSignOutResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the protected endpoint response status code should be {int}")
    public void theProtectedEndpointResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the revoked token should be stored only as a hash")
    public void theRevokedTokenShouldBeStoredOnlyAsAHash() {
        assertTrue(revokedTokenRepository.existsByTokenHash(RevokedToken.hash(token)));
        var rawTokenRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM revoked_tokens WHERE token_hash = ?", Integer.class, token);
        assertEquals(0, rawTokenRows);
    }

    @Then("the other user's token is still accepted by a protected endpoint")
    public void theOtherUsersTokenIsStillAccepted() throws Exception {
        assertEquals(200, requestProtectedEndpoint(otherToken).getResponse().getStatus());
    }

    private String registerUserAndIssueToken() {
        var user = userRepository.save(new User("signout.user." + NEXT_SUFFIX.getAndIncrement(), "encoded-password"));
        return tokenService.generateToken(user.getUsername());
    }

    private MvcResult signOut(String authorizationHeader) throws Exception {
        return mockMvc.perform(post(SIGN_OUT_URL).header("Authorization", authorizationHeader)).andReturn();
    }

    private MvcResult requestProtectedEndpoint(String bearerToken) throws Exception {
        return mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + bearerToken)).andReturn();
    }
}
