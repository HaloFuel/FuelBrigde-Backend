package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.notification.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotificationLifecycleSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    private Long notificationId;
    private HttpStatusCode lastStatus;
    private Map<?, ?> lastBody;

    @Given("buyer company {int} is registered as user {int} in IAM")
    public void buyerCompanyIsRegisteredAsUserInIam(int companyId, int userId) {
        // The IAM user directory stub, started in CucumberSpringConfiguration,
        // already maps buyer company 101 to user 1101; nothing else to set up.
    }

    @When("a {string} notification referencing order {int} is created for buyer company {int}")
    public void aNotificationIsCreatedForBuyerCompany(String type, int orderId, int companyId) {
        createNotification(type, orderId, companyId);
    }

    @Given("a {string} notification referencing order {int} exists for buyer company {int}")
    public void aNotificationExistsForBuyerCompany(String type, int orderId, int companyId) {
        createNotification(type, orderId, companyId);
        assertEquals(201, lastStatus.value());
        notificationId = ((Number) lastBody.get("id")).longValue();
    }

    @When("the notification is marked as read")
    public void theNotificationIsMarkedAsRead() {
        try {
            var response = client().post().uri("/api/v1/notifications/" + notificationId + "/mark-as-read")
                    .retrieve().toEntity(Map.class);
            lastStatus = response.getStatusCode();
            lastBody = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode();
            lastBody = null;
        }
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus.value());
    }

    @Then("the notification should be unread")
    public void theNotificationShouldBeUnread() {
        assertEquals(false, lastBody.get("read"));
    }

    @Then("the notification should be read")
    public void theNotificationShouldBeRead() {
        assertEquals(true, lastBody.get("read"));
    }

    private void createNotification(String type, int orderId, int companyId) {
        Map<String, Object> request = new HashMap<>();
        request.put("companyId", companyId);
        request.put("type", type);
        request.put("title", "Pedido despachado");
        request.put("message", "El pedido número " + orderId + " ha sido despachado.");
        request.put("referenceId", orderId);
        try {
            var response = client().post().uri("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve().toEntity(Map.class);
            lastStatus = response.getStatusCode();
            lastBody = response.getBody();
            notificationId = ((Number) lastBody.get("id")).longValue();
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
