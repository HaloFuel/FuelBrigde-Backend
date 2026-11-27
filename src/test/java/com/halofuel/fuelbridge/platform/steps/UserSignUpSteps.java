package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Steps for user-sign-up.feature (TS-06): POST /api/v1/authentication/sign-up. The request goes through
 * the real security chain without any authentication, as the endpoint is public. Company ids start at
 * 880201 so they never collide with those of the other features sharing the H2 database.
 */
public class UserSignUpSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MvcResult response;

    @When("a client signs up with username {string} and password {string}")
    public void aClientSignsUp(String username, String password) throws Exception {
        signUp(username, password, null);
    }

    @When("a client signs up with username {string} and password {string} for company {long}")
    public void aClientSignsUpForCompany(String username, String password, Long companyId) throws Exception {
        signUp(username, password, companyId);
    }

    @Then("the sign-up response status code should be {int}")
    public void theSignUpResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the sign-up error should be on field {string} with message containing {string}")
    public void theSignUpErrorShouldBeOnField(String field, String text) throws Exception {
        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
        assertEquals("VALIDATION_ERROR", body.get("code").asText());
        // ErrorResource carries the field in "message" and the reason in "details".
        assertTrue(body.get("message").asText().contains(field), body.toString());
        assertTrue(body.get("details").asText().contains(text), body.toString());
    }

    @Then("the sign-up response should show the username {string} and company {long}")
    public void theSignUpResponseShouldShow(String username, Long companyId) throws Exception {
        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
        assertEquals(username, body.get("username").asText());
        assertEquals(companyId, body.get("companyId").asLong());
        assertFalse(body.has("password"));
    }

    @Then("no user named {string} is stored")
    public void noUserNamedIsStored(String username) {
        assertFalse(userRepository.existsByUsername(username));
    }

    @Then("a user named {string} is stored")
    public void aUserNamedIsStored(String username) {
        assertTrue(userRepository.existsByUsername(username));
    }

    private void signUp(String username, String password, Long companyId) throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("roles", List.of("ROLE_BUYER"));
        payload.put("companyId", companyId);
        payload.put("providerId", null);
        response = mockMvc.perform(
                post("/api/v1/authentication/sign-up")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
        ).andReturn();
    }
}
