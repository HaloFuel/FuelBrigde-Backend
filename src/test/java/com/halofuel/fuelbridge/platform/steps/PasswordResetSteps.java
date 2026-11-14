package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.PasswordResetToken;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.PasswordResetTokenRepository;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Steps for password-reset.feature (TS-02). The endpoints are public, so requests carry no
 * authentication; sign-in is exercised through the real endpoint to prove the new password works.
 */
public class PasswordResetSteps {

    private static final String FORGOT_URL = "/api/v1/authentication/forgot-password";
    private static final String RESET_URL = "/api/v1/authentication/reset-password";
    private static final String SIGN_IN_URL = "/api/v1/authentication/sign-in";

    // static: Cucumber creates a new glue instance per scenario and username is unique in users.
    private static final AtomicLong NEXT_SUFFIX = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    // Two beans of this type exist (the service and the Spring Security passwordEncoder alias of it).
    @Autowired
    @Qualifier("hashingServiceImpl")
    private HashingService hashingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User user;
    private String resetToken;
    private String firstResetToken;
    private MvcResult response;

    @Given("a registered user with password {string}")
    public void aRegisteredUserWithPassword(String password) {
        user = userRepository.save(new User(
                "reset.user." + NEXT_SUFFIX.getAndIncrement() + "@empresa.pe", hashingService.encode(password)));
        assertNotNull(user.getId());
    }

    @Given("the user requested a password reset")
    public void theUserRequestedAPasswordReset() throws Exception {
        requestReset(Map.of("username", user.getUsername()));
        assertEquals(200, response.getResponse().getStatus());
        resetToken = tokenFromResponse();
    }

    @Given("the user requested a password reset again")
    public void theUserRequestedAPasswordResetAgain() throws Exception {
        firstResetToken = resetToken;
        theUserRequestedAPasswordReset();
    }

    @Given("the user has an expired reset token")
    public void theUserHasAnExpiredResetToken() {
        resetToken = UUID.randomUUID().toString();
        tokenRepository.save(new PasswordResetToken(user.getId(), resetToken, Instant.now().minus(Duration.ofMinutes(1))));
    }

    @Given("the user already reset the password to {string} with that token")
    public void theUserAlreadyResetThePasswordWithThatToken(String newPassword) throws Exception {
        reset(resetToken, newPassword);
        assertEquals(200, response.getResponse().getStatus());
    }

    @When("the user requests a password reset by username")
    public void theUserRequestsAPasswordResetByUsername() throws Exception {
        requestReset(Map.of("username", user.getUsername()));
    }

    @When("the user requests a password reset by email")
    public void theUserRequestsAPasswordResetByEmail() throws Exception {
        requestReset(Map.of("email", user.getUsername()));
    }

    @When("someone requests a password reset for {string}")
    public void someoneRequestsAPasswordResetFor(String identifier) throws Exception {
        requestReset(Map.of("email", identifier));
    }

    @When("the user resets the password to {string} with that token")
    public void theUserResetsThePasswordWithThatToken(String newPassword) throws Exception {
        reset(resetToken, newPassword);
    }

    @When("the user resets the password to {string} with the first token")
    public void theUserResetsThePasswordWithTheFirstToken(String newPassword) throws Exception {
        reset(firstResetToken, newPassword);
    }

    @When("someone resets the password to {string} with token {string}")
    public void someoneResetsThePasswordWithToken(String newPassword, String token) throws Exception {
        reset(token, newPassword);
    }

    @Then("the password reset response status code should be {int}")
    public void thePasswordResetResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the response should contain a reset token valid for one hour")
    public void theResponseShouldContainAResetTokenValidForOneHour() throws Exception {
        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
        assertEquals(36, UUID.fromString(body.get("token").asText()).toString().length());
        var expiresAt = Instant.parse(body.get("expiresAt").asText());
        var remaining = Duration.between(Instant.now(), expiresAt);
        assertTrue(remaining.compareTo(Duration.ofMinutes(59)) > 0 && remaining.compareTo(Duration.ofMinutes(61)) < 0,
                "expiresAt should be about one hour away but was " + remaining);
    }

    @Then("the reset token should be stored only as a hash")
    public void theResetTokenShouldBeStoredOnlyAsAHash() throws Exception {
        var token = tokenFromResponse();
        assertTrue(tokenRepository.findByTokenHash(PasswordResetToken.hash(token)).isPresent());
        var rawRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash = ?", Integer.class, token);
        assertEquals(0, rawRows);
    }

    @Then("the user can sign in with {string}")
    public void theUserCanSignInWith(String password) throws Exception {
        assertEquals(200, signIn(password).getResponse().getStatus());
    }

    @Then("the user cannot sign in with {string}")
    public void theUserCannotSignInWith(String password) throws Exception {
        assertEquals(400, signIn(password).getResponse().getStatus());
    }

    @Then("the reset token should be marked as used")
    public void theResetTokenShouldBeMarkedAsUsed() {
        assertTrue(storedResetToken().isUsed());
    }

    @Then("the reset token should not be marked as used")
    public void theResetTokenShouldNotBeMarkedAsUsed() {
        assertFalse(storedResetToken().isUsed());
    }

    private PasswordResetToken storedResetToken() {
        return tokenRepository.findByTokenHash(PasswordResetToken.hash(resetToken)).orElseThrow();
    }

    private String tokenFromResponse() throws Exception {
        return objectMapper.readTree(response.getResponse().getContentAsString()).get("token").asText();
    }

    private void requestReset(Map<String, String> body) throws Exception {
        response = mockMvc.perform(post(FORGOT_URL).contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))).andReturn();
    }

    private void reset(String token, String newPassword) throws Exception {
        response = mockMvc.perform(post(RESET_URL).contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token, "newPassword", newPassword))))
                .andReturn();
    }

    private MvcResult signIn(String password) throws Exception {
        return mockMvc.perform(post(SIGN_IN_URL).contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", user.getUsername(), "password", password))))
                .andReturn();
    }
}
