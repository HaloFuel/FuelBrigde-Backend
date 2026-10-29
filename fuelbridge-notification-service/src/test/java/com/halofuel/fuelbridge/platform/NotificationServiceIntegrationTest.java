package com.halofuel.fuelbridge.platform;

import com.halofuel.fuelbridge.platform.notification.infrastructure.security.tokens.jwt.BearerTokenService;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {
    private static final String SECRET = "test-secret-for-notification-jwt-validation-at-least-64-bytes-long!!";
    private static final AtomicReference<String> directoryAuthorization = new AtomicReference<>();
    private static final HttpServer directory = startDirectory();
    @LocalServerPort private int port;
    @Autowired private BearerTokenService tokens;

    private static HttpServer startDirectory() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/users", exchange -> {
                directoryAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                byte[] response = "[{\"id\":42,\"username\":\"buyer\",\"roles\":[\"ROLE_BUYER\"],\"companyId\":7,\"providerId\":9}]".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DynamicPropertySource
    static void directoryProperties(DynamicPropertyRegistry registry) {
        registry.add("iam.service.url", () -> "http://127.0.0.1:" + directory.getAddress().getPort());
    }

    @AfterAll
    static void shutdownDirectory() { directory.stop(0); }

    private RestClient client(String token) {
        var builder = RestClient.builder().baseUrl("http://127.0.0.1:" + port);
        if (token != null) builder.defaultHeader("Authorization", "Bearer " + token);
        return builder.build();
    }

    @Test
    void signedMonolithFormatJwtCreatesReadsAndMarksNotification() {
        // Same jjwt subject, UTF-8 HMAC key and signing algorithm as the monolith.
        var jwt = Jwts.builder().subject("buyer").issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        var client = client(jwt);
        var created = client.post().uri("/api/v1/notifications").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userId", 42, "type", "ORDER_CONFIRMED", "title", "Confirmed", "message", "Order 11", "referenceId", 11))
                .retrieve().toEntity(Map.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        var body = created.getBody();
        assertNotNull(body);
        assertEquals(42, ((Number) body.get("userId")).intValue());
        assertEquals("ORDER_CONFIRMED", body.get("type"));
        assertNotNull(body.get("createdAt"));
        var id = body.get("id");
        var marked = client.post().uri("/api/v1/notifications/" + id + "/mark-as-read")
                .retrieve().body(Map.class);
        assertEquals(true, marked.get("read"));
        var found = client.get().uri("/api/v1/notifications/" + id).retrieve().body(Map.class);
        assertEquals(true, found.get("read"));
    }

    @Test
    void companyAndProviderRoutesUseExistingIamContractAndForwardBearer() {
        var jwt = tokens.generateToken("buyer");
        var client = client(jwt);
        for (var recipient : Map.of("companyId", 7, "providerId", 9).entrySet()) {
            var created = client.post().uri("/api/v1/notifications").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(recipient.getKey(), recipient.getValue(), "type", "GENERAL", "title", "Hello", "message", "Message"))
                    .retrieve().body(Map.class);
            assertEquals(42, ((Number) created.get("userId")).intValue());
        }
        assertEquals(HttpStatus.OK, client.get().uri("/api/v1/notifications/buyer/7").retrieve().toBodilessEntity().getStatusCode());
        assertEquals(HttpStatus.OK, client.get().uri("/api/v1/notifications/provider/9").retrieve().toBodilessEntity().getStatusCode());
        assertEquals("Bearer " + jwt, directoryAuthorization.get());
    }

    @Test
    void rejectsMissingExpiredAndWrongSignatureTokens() {
        var expired = Jwts.builder().subject("buyer").expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        var foreign = Jwts.builder().subject("buyer").expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("another-secret-for-jwt-validation-with-at-least-64-bytes-long!!!!!!".getBytes(StandardCharsets.UTF_8))).compact();
        for (var jwt : new String[] {null, expired, foreign, "malformed"}) {
            var error = assertThrows(RestClientResponseException.class,
                    () -> client(jwt).get().uri("/api/v1/notifications/user/42").retrieve().toBodilessEntity());
            assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        }
    }

    @Test
    void unknownNotificationReturnsNotFound() {
        var error = assertThrows(RestClientResponseException.class, () -> client(tokens.generateToken("buyer"))
                .post().uri("/api/v1/notifications/999999/mark-as-read").retrieve().toBodilessEntity());
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
