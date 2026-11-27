package com.halofuel.fuelbridge.platform.ordering.infrastructure.notification;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.CompanyUsers;
import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.IntegrationEventPublisher.OrderIntegrationEvent;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** T10.04b: the Notification adapter of the integration event port. */
class NotificationServiceEventPublisherTest {

    private final List<String> requests = new CopyOnWriteArrayList<>();
    private volatile int responseStatus = 201;
    private HttpServer server;
    private CompanyUsers users;
    private NotificationServiceEventPublisher publisher;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/notifications", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestHeaders().getFirst("Authorization")
                    + " key=" + exchange.getRequestHeaders().getFirst("Idempotency-Key") + " "
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
        users = mock(CompanyUsers.class);
        var tokens = mock(ServiceTokenIssuer.class);
        when(tokens.issueFor("fuelbridge-ordering-service")).thenReturn("signed-jwt");
        publisher = new NotificationServiceEventPublisher(users, tokens,
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static OrderIntegrationEvent event(String id, String type, long orderId, long companyId) {
        return new OrderIntegrationEvent(id, type, orderId, companyId, Instant.parse("2026-11-26T15:00:00Z"));
    }

    @Test
    void sendsEveryOrderEventToTheBuyerWithServiceTokenAndIdempotencyKey() {
        when(users.findUserIdByCompanyId(7L)).thenReturn(Optional.of(42L));

        publisher.publish(event("e-1", "FuelOrderConfirmed", 11L, 7L));
        publisher.publish(event("e-2", "FuelOrderCancelled", 12L, 7L));
        publisher.publish(event("e-3", "FuelOrderDispatched", 13L, 7L));

        assertEquals(3, requests.size());
        requests.forEach(request -> {
            assertTrue(request.startsWith("POST Bearer signed-jwt key=e-"));
            assertTrue(request.contains("\"userId\":42"));
        });
        assertTrue(requests.get(0).contains("ORDER_CONFIRMED"));
        assertTrue(requests.get(1).contains("ORDER_CANCELLED"));
        assertTrue(requests.get(2).contains("ORDER_DISPATCHED"));
        assertTrue(requests.get(2).contains("\"referenceId\":13"));
    }

    @Test
    void aCompanyWithoutUserIsDeliveredWithoutARequest() {
        when(users.findUserIdByCompanyId(99L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> publisher.publish(event("e-4", "FuelOrderConfirmed", 14L, 99L)));

        assertTrue(requests.isEmpty());
    }

    @Test
    void aNotificationServiceErrorIsThrownBackForARetry() {
        when(users.findUserIdByCompanyId(7L)).thenReturn(Optional.of(42L));
        responseStatus = 503;

        assertThrows(RestClientResponseException.class,
                () -> publisher.publish(event("e-5", "FuelOrderDispatched", 15L, 7L)));
    }

    @Test
    void anUnreachableIdentityServiceIsThrownBackForARetry() {
        when(users.findUserIdByCompanyId(7L)).thenThrow(new IllegalStateException("IAM unavailable"));

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(event("e-6", "FuelOrderDispatched", 16L, 7L)));
        assertTrue(requests.isEmpty());
    }

    @Test
    void anUnknownEventTypeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(event("e-7", "SomethingElse", 17L, 7L)));
        verifyNoInteractions(users);
    }
}
