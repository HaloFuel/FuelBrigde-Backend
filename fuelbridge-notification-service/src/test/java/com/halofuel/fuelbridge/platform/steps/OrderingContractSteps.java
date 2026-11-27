package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.notification.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provider side of the Ordering -> Notification contract (T09.13). The request body
 * mirrors NotificationServiceClient in the platform: it carries the recipient's userId
 * directly, so the service must accept it without resolving a company through IAM.
 */
public class OrderingContractSteps {

    /** Subject used by NotificationServiceClient when it signs its service token. */
    private static final String PUBLISHER_IDENTITY = "fuelbridge-notification-publisher";

    private static final ParameterizedTypeReference<Map<String, Object>> NOTIFICATION =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> NOTIFICATIONS =
            new ParameterizedTypeReference<>() { };

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    private String publisherToken;
    private int lastStatus;
    private Map<String, Object> lastNotification;
    private List<Map<String, Object>> unread;

    @Given("the Ordering publisher is authenticated with its service token")
    public void theOrderingPublisherIsAuthenticated() {
        publisherToken = tokens.generateToken(PUBLISHER_IDENTITY);
    }

    @When("the publisher notifies user {int} that order {int} was {string} with type {string}")
    public void thePublisherNotifiesUser(int userId, int orderId, String status, String type) {
        // Same fields, in the same shape, as NotificationServiceClient.NotificationRequest.
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userId", userId);
        request.put("type", type);
        request.put("title", "Pedido " + status);
        request.put("message", "El pedido número " + orderId + " ha sido " + status + ".");
        request.put("referenceId", orderId);
        try {
            var response = client().post().uri("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve().toEntity(NOTIFICATION);
            lastStatus = response.getStatusCode().value();
            lastNotification = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
            lastNotification = null;
        }
    }

    @When("user {int} checks the unread notifications")
    public void userChecksTheUnreadNotifications(int userId) {
        unread = client().get().uri("/api/v1/notifications/user/{id}/unread", userId)
                .retrieve().body(NOTIFICATIONS);
    }

    @Then("the publisher's request is accepted with status {int}")
    public void thePublishersRequestIsAccepted(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus);
    }

    @Then("the stored notification is addressed to user {int}")
    public void theStoredNotificationIsAddressedTo(int userId) {
        assertNotNull(lastNotification, "No notification was returned");
        assertEquals((long) userId, ((Number) lastNotification.get("userId")).longValue());
    }

    @Then("the stored notification is an unread {string} about order {int}")
    public void theStoredNotificationIsAnUnread(String type, int orderId) {
        assertEquals(type, lastNotification.get("type"));
        assertEquals((long) orderId, ((Number) lastNotification.get("referenceId")).longValue());
        assertEquals(false, lastNotification.get("read"));
    }

    @Then("the unread notifications include order {int}")
    public void theUnreadNotificationsIncludeOrder(int orderId) {
        assertNotNull(unread, "No unread notifications were returned");
        assertTrue(unread.stream().anyMatch(n -> ((Number) n.get("referenceId")).longValue() == orderId),
                "Order " + orderId + " is not among the unread notifications");
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .defaultHeader("Authorization", "Bearer " + publisherToken)
                .build();
    }
}
