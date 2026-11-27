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
 * Covers the read side of the notification service (TS-26): lookup by id and the
 * user, buyer, provider and unread listings. Notifications are created through
 * the public API so every scenario exercises the same contract the frontend uses.
 */
public class NotificationQuerySteps {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_NOTIFICATIONS =
            new ParameterizedTypeReference<>() { };

    @LocalServerPort
    private int port;

    @Autowired
    private BearerTokenService tokens;

    /** Notification id created in this scenario, keyed by the order it refers to. */
    private final Map<Long, Long> notificationIdByOrder = new HashMap<>();

    private int lastStatus;
    private Map<String, Object> lastNotification;
    private List<Map<String, Object>> lastList;

    @Given("provider {int} is registered as user {int} in IAM")
    public void providerIsRegisteredAsUserInIam(int providerId, int userId) {
        // The IAM user directory stub in CucumberSpringConfiguration maps provider 202 to user 2202.
    }

    @Given("buyer company {int} has received a {string} notification about order {int}")
    public void buyerCompanyHasReceivedNotification(int companyId, String type, int orderId) {
        var request = baseRequest(type, orderId);
        request.put("companyId", companyId);
        create(request, orderId);
    }

    @Given("provider {int} has received a {string} notification about order {int}")
    public void providerHasReceivedNotification(int providerId, String type, int orderId) {
        var request = baseRequest(type, orderId);
        request.put("providerId", providerId);
        create(request, orderId);
    }

    @Given("the notification about order {int} has been read")
    public void theNotificationAboutOrderHasBeenRead(int orderId) {
        var response = client().post()
                .uri("/api/v1/notifications/{id}/mark-as-read", idOf(orderId))
                .retrieve().toBodilessEntity();
        assertEquals(200, response.getStatusCode().value());
    }

    @When("I request the notification about order {int}")
    public void iRequestTheNotificationAboutOrder(int orderId) {
        getOne(idOf(orderId));
    }

    @When("I request the notification with id {int}")
    public void iRequestTheNotificationWithId(int notificationId) {
        getOne((long) notificationId);
    }

    @When("I list the notifications of user {int}")
    public void iListTheNotificationsOfUser(int userId) {
        getList("/api/v1/notifications/user/{id}", userId);
    }

    @When("I list the notifications of buyer company {int}")
    public void iListTheNotificationsOfBuyerCompany(int companyId) {
        getList("/api/v1/notifications/buyer/{id}", companyId);
    }

    @When("I list the notifications of provider {int}")
    public void iListTheNotificationsOfProvider(int providerId) {
        getList("/api/v1/notifications/provider/{id}", providerId);
    }

    @When("I list the unread notifications of user {int}")
    public void iListTheUnreadNotificationsOfUser(int userId) {
        getList("/api/v1/notifications/user/{id}/unread", userId);
    }

    @Then("the query responds with status {int}")
    public void theQueryRespondsWithStatus(int expectedStatus) {
        assertEquals(expectedStatus, lastStatus);
    }

    @Then("the returned notification refers to order {int} with type {string}")
    public void theReturnedNotificationRefersToOrder(int orderId, String type) {
        assertNotNull(lastNotification, "No notification was returned");
        assertEquals((long) orderId, ((Number) lastNotification.get("referenceId")).longValue());
        assertEquals(type, lastNotification.get("type"));
    }

    @Then("the list contains notifications about orders {string}")
    public void theListContainsNotificationsAboutOrders(String orderIds) {
        Set<Long> expected = Arrays.stream(orderIds.split(","))
                .map(String::trim).map(Long::valueOf).collect(Collectors.toSet());
        assertEquals(expected, referenceIdsOfLastList());
    }

    @Then("the list is empty")
    public void theListIsEmpty() {
        assertNotNull(lastList, "No list was returned");
        assertTrue(lastList.isEmpty(), "Expected no notifications but got " + lastList.size());
    }

    private Map<String, Object> baseRequest(String type, int orderId) {
        Map<String, Object> request = new HashMap<>();
        request.put("type", type);
        request.put("title", "Actualización del pedido");
        request.put("message", "El pedido número " + orderId + " cambió de estado.");
        request.put("referenceId", orderId);
        return request;
    }

    private void create(Map<String, Object> request, int orderId) {
        var response = client().post().uri("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve().toEntity(new ParameterizedTypeReference<Map<String, Object>>() { });
        assertEquals(201, response.getStatusCode().value());
        notificationIdByOrder.put((long) orderId, ((Number) response.getBody().get("id")).longValue());
    }

    private void getOne(Long notificationId) {
        try {
            var response = client().get().uri("/api/v1/notifications/{id}", notificationId)
                    .retrieve().toEntity(new ParameterizedTypeReference<Map<String, Object>>() { });
            lastStatus = response.getStatusCode().value();
            lastNotification = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
            lastNotification = null;
        }
    }

    private void getList(String uri, int id) {
        try {
            var response = client().get().uri(uri, id).retrieve().toEntity(LIST_OF_NOTIFICATIONS);
            lastStatus = response.getStatusCode().value();
            lastList = response.getBody();
        } catch (RestClientResponseException ex) {
            lastStatus = ex.getStatusCode().value();
            lastList = null;
        }
    }

    private Set<Long> referenceIdsOfLastList() {
        assertNotNull(lastList, "No list was returned");
        return lastList.stream()
                .map(notification -> ((Number) notification.get("referenceId")).longValue())
                .collect(Collectors.toSet());
    }

    private Long idOf(int orderId) {
        var id = notificationIdByOrder.get((long) orderId);
        assertNotNull(id, "No notification about order " + orderId + " was created in this scenario");
        return id;
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .defaultHeader("Authorization", "Bearer " + tokens.generateToken("buyer"))
                .build();
    }
}
