package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Steps for user-profile-edit.feature (TS-10): PUT /api/v1/users/{userId}/profile.
 */
public class UserProfileEditSteps {

    // static: Cucumber creates a new glue instance per scenario and username is unique in users.
    private static final AtomicLong NEXT_SUFFIX = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User registeredUser;
    private MvcResult response;

    @Given("a registered user named {string}")
    public void aRegisteredUserNamed(String username) {
        registeredUser = userRepository.save(
                new User(username + "." + NEXT_SUFFIX.getAndIncrement(), "encoded-password"));
        assertNotNull(registeredUser.getId());
    }

    @When("the user {string} updates the profile of the registered user with full name {string} and phone {string}")
    public void theUserUpdatesTheProfileOfTheRegisteredUser(String caller, String fullName, String phone)
            throws Exception {
        // The registered user's real username carries a unique suffix, so the owner is
        // identified by its prefix.
        var authenticatedName = registeredUser.getUsername().startsWith(caller + ".")
                ? registeredUser.getUsername()
                : caller;
        updateProfile(authenticatedName, registeredUser.getId(), fullName, phone);
    }

    @When("the user {string} updates the profile of user {long} with full name {string} and phone {string}")
    public void theUserUpdatesTheProfileOfUser(String caller, Long userId, String fullName, String phone)
            throws Exception {
        updateProfile(caller, userId, fullName, phone);
    }

    @Then("the profile response status code should be {int}")
    public void theProfileResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the profile response should show full name {string} and phone {string}")
    public void theProfileResponseShouldShow(String fullName, String phone) throws Exception {
        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
        assertEquals(registeredUser.getId(), body.get("id").asLong());
        assertEquals(fullName, body.get("fullName").asText());
        assertEquals(phone, body.get("phone").asText());
    }

    @Then("the stored profile of the registered user should have full name {string} and phone {string}")
    public void theStoredProfileShouldHave(String fullName, String phone) {
        var stored = userRepository.findById(registeredUser.getId()).orElseThrow();
        assertEquals(fullName, stored.getFullName());
        assertEquals(phone, stored.getPhone());
    }

    @Then("the stored profile of the registered user should have no full name")
    public void theStoredProfileShouldHaveNoFullName() {
        var stored = userRepository.findById(registeredUser.getId()).orElseThrow();
        assertNull(stored.getFullName());
    }

    private void updateProfile(String authenticatedName, Long userId, String fullName, String phone)
            throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("fullName", fullName, "phone", phone));
        response = mockMvc.perform(
                put("/api/v1/users/{userId}/profile", userId).with(user(authenticatedName))
                        .contentType(APPLICATION_JSON)
                        .content(body)
        ).andReturn();
    }
}
