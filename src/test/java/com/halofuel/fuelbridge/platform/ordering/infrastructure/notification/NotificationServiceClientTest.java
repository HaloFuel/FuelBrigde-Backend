package com.halofuel.fuelbridge.platform.ordering.infrastructure.notification;

import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceClientTest {
    @Test
    void sendsAllOrderEventsWithRecipientAndBearerToken() throws Exception {
        var requests = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/notifications", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestHeaders().getFirst("Authorization")
                    + " " + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        try {
            var users = mock(UserRepository.class);
            var tokens = mock(TokenService.class);
            var user = new User();
            user.setId(42L);
            when(users.findByCompanyId(7L)).thenReturn(Optional.of(user));
            when(tokens.generateToken("fuelbridge-notification-publisher")).thenReturn("signed-jwt");
            var client = new NotificationServiceClient(users, tokens, "http://127.0.0.1:" + server.getAddress().getPort());
            client.on(new FuelOrderConfirmedEvent(11L, 7L));
            client.on(new FuelOrderCancelledEvent(12L, 7L));
            client.on(new FuelOrderDispatchedEvent(13L, 7L));
            assertEquals(3, requests.size());
            for (var request : requests) {
                assertTrue(request.startsWith("POST Bearer signed-jwt "));
                assertTrue(request.contains("\"userId\":42"));
            }
            assertTrue(requests.get(0).contains("ORDER_CONFIRMED"));
            assertTrue(requests.get(1).contains("ORDER_CANCELLED"));
            assertTrue(requests.get(2).contains("ORDER_DISPATCHED"));
            assertTrue(requests.get(2).contains("\"referenceId\":13"));
            when(users.findByCompanyId(99L)).thenReturn(Optional.empty());
            client.on(new FuelOrderConfirmedEvent(14L, 99L));
            assertEquals(3, requests.size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void surfacesHttpFailureToPublisher() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/notifications", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        try {
            var users = mock(UserRepository.class);
            var tokens = mock(TokenService.class);
            var user = new User();
            user.setId(42L);
            when(users.findByCompanyId(7L)).thenReturn(Optional.of(user));
            when(tokens.generateToken(anyString())).thenReturn("signed-jwt");
            var client = new NotificationServiceClient(users, tokens, "http://127.0.0.1:" + server.getAddress().getPort());
            assertThrows(RestClientResponseException.class, () -> client.on(new FuelOrderConfirmedEvent(11L, 7L)));
        } finally {
            server.stop(0);
        }
    }
}
