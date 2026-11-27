package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Steps for user-lookup-by-company.feature (TS-10): GET /api/v1/users/company/{companyId}.
 * Company ids start at 880101 so they never collide with the ids of the other features that
 * share the same H2 database.
 */
public class UserLookupByCompanySteps {

    // static: Cucumber creates a new glue instance per scenario and username is unique in users.
    private static final AtomicLong NEXT_SUFFIX = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Value("${authorization.jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User registeredUser;
    private MvcResult response;

    @Given("a registered user named {string} linked to company {long}")
    public void aRegisteredUserLinkedToCompany(String username, Long companyId) {
        var user = new User(username + "." + NEXT_SUFFIX.getAndIncrement(), "encoded-password");
        user.setCompanyId(companyId);
        registeredUser = userRepository.save(user);
        assertNotNull(registeredUser.getId());
    }

    @Given("no user is linked to company {long}")
    public void noUserIsLinkedToCompany(Long companyId) {
        assertTrue(userRepository.findByCompanyId(companyId).isEmpty());
    }

    @When("the operator requests the user of company {long}")
    public void theOperatorRequestsTheUserOfCompany(Long companyId) throws Exception {
        response = mockMvc.perform(
                get("/api/v1/users/company/{companyId}", companyId).with(user("operator"))
        ).andReturn();
    }

    @When("the service {string} requests the user of company {long} with its service token")
    public void theServiceRequestsTheUserOfCompany(String serviceName, Long companyId) throws Exception {
        // A real signed token goes through the Bearer filter, so ROLE_SERVICE is exercised end to end.
        var token = new ServiceTokenIssuer(jwtSecret).issueFor(serviceName);
        response = mockMvc.perform(
                get("/api/v1/users/company/{companyId}", companyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        ).andReturn();
    }

    @When("an anonymous client requests the user of company {long}")
    public void anAnonymousClientRequestsTheUserOfCompany(Long companyId) throws Exception {
        response = mockMvc.perform(get("/api/v1/users/company/{companyId}", companyId)).andReturn();
    }

    @Then("the lookup response status code should be {int}")
    public void theLookupResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the lookup response should show the registered user linked to company {long}")
    public void theLookupResponseShouldShowTheRegisteredUser(Long companyId) throws Exception {
        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
        assertEquals(registeredUser.getId(), body.get("id").asLong());
        assertEquals(registeredUser.getUsername(), body.get("username").asText());
        assertEquals(companyId, body.get("companyId").asLong());
        assertTrue(body.get("providerId").isNull());
    }
}
