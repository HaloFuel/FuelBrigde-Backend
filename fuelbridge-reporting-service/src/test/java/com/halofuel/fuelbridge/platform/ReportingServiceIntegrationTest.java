package com.halofuel.fuelbridge.platform;

import com.halofuel.fuelbridge.platform.reporting.infrastructure.security.tokens.jwt.BearerTokenService;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReportingServiceIntegrationTest {
    private static final String SECRET = "test-secret-for-service-jwt-validation-at-least-64-bytes-long!!!!!!";
    private static final AtomicReference<String> authorization = new AtomicReference<>();
    private static final AtomicBoolean unavailable = new AtomicBoolean();
    private static final String ORDERS = """
            [{"id":1,"status":"CONFIRMED","companyId":7,"providerId":9},
             {"id":2,"status":"CANCELLED","companyId":7,"providerId":9},
             {"id":3,"status":"DELIVERED","companyId":7,"providerId":9}]
            """;
    private static final String ALL_ORDERS = ORDERS.strip().replace("]", ",{" + "\"id\":4,\"status\":\"PENDING\"}]");
    private static final String PAYMENTS = """
            [{"id":1,"orderId":1,"amount":100,"status":"COMPLETED","paidAt":"2026-01-15T10:00:00"},
             {"id":2,"orderId":3,"amount":50,"status":"COMPLETED","paidAt":"2026-02-01T10:00:00"},
             {"id":3,"orderId":1,"amount":25,"status":"COMPLETED","paidAt":"2026-01-20T10:00:00"},
             {"id":4,"orderId":2,"amount":30,"status":"PENDING","paidAt":null},
             {"id":6,"orderId":3,"amount":null,"status":"COMPLETED","paidAt":null}]
            """;
    private static final String ALL_PAYMENTS = PAYMENTS.strip().replace("]",
            ",{" + "\"id\":5,\"orderId\":99,\"amount\":900,\"status\":\"COMPLETED\",\"paidAt\":null}]");
    private static final HttpServer source = startSource();
    @LocalServerPort private int port;
    @Autowired private BearerTokenService tokens;

    private static HttpServer startSource() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/", exchange -> {
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                if (unavailable.get()) {
                    exchange.sendResponseHeaders(503, -1);
                    exchange.close();
                    return;
                }
                String body = switch (exchange.getRequestURI().getPath()) {
                    case "/api/v1/fuel-orders" -> ALL_ORDERS;
                    case "/api/v1/fuel-orders/company/7", "/api/v1/fuel-orders/provider/9" -> ORDERS;
                    case "/api/v1/payments" -> ALL_PAYMENTS;
                    case "/api/v1/payments/company/7" -> PAYMENTS;
                    case "/api/v1/deliveries" -> "[{\"id\":1,\"status\":\"DELIVERED\"},{\"id\":2,\"status\":\"DISPATCHED\"}]";
                    default -> "[]";
                };
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("fuelbridge.api.url", () -> "http://127.0.0.1:" + source.getAddress().getPort());
    }

    @AfterAll static void stopSource() { source.stop(0); }

    private RestClient client(String jwt) {
        var builder = RestClient.builder().baseUrl("http://127.0.0.1:" + port);
        if (jwt != null) builder.defaultHeader("Authorization", "Bearer " + jwt);
        return builder.build();
    }

    private Map<?, ?> analytics(RestClient client, String suffix) {
        return client.get().uri("/api/v1/analytics/" + suffix).retrieve().body(Map.class);
    }

    @Test
    void preservesProviderRevenueOrderCountsAndMonthlyGrouping() {
        var jwt = Jwts.builder().subject("provider").issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        var result = analytics(client(jwt), "providers/9");
        assertEquals(3, ((Number) result.get("totalOrders")).intValue());
        assertEquals(2, ((Number) result.get("confirmedOrders")).intValue());
        assertEquals(1, ((Number) result.get("cancelledOrders")).intValue());
        assertEquals(175.0, ((Number) result.get("totalRevenue")).doubleValue());
        var months = (List<Map<String, Object>>) result.get("monthlyRevenue");
        assertEquals(2, months.size());
        assertEquals("2026-01", months.get(0).get("month"));
        assertEquals(125.0, ((Number) months.get(0).get("amount")).doubleValue());
        assertEquals("2026-02", months.get(1).get("month"));
        assertEquals(50.0, ((Number) months.get(1).get("amount")).doubleValue());
        assertEquals("Bearer " + jwt, authorization.get());
    }

    @Test
    void preservesBuyerAndPlatformKpisAndEmptyDatasets() {
        var client = client(tokens.generateToken("buyer"));
        var buyer = analytics(client, "buyers/7");
        assertEquals(175.0, ((Number) buyer.get("totalSpent")).doubleValue());
        assertEquals(4, ((Number) buyer.get("completedPayments")).intValue());
        assertEquals(1, ((Number) buyer.get("pendingPayments")).intValue());
        var platform = analytics(client, "platform");
        assertEquals(4, ((Number) platform.get("totalOrders")).intValue());
        assertEquals(6, ((Number) platform.get("totalPayments")).intValue());
        assertEquals(1075.0, ((Number) platform.get("totalRevenue")).doubleValue());
        assertEquals(1, ((Number) platform.get("pendingOrders")).intValue());
        assertEquals(2, ((Number) platform.get("totalDeliveries")).intValue());
        assertEquals(1, ((Number) platform.get("completedDeliveries")).intValue());
        var empty = analytics(client, "buyers/99");
        assertEquals(0, ((Number) empty.get("totalOrders")).intValue());
        assertEquals(List.of(), empty.get("monthlySpending"));
    }

    @Test
    void reportsSourceOutagesInsteadOfReturningMisleadingZeroKpis() {
        unavailable.set(true);
        try {
            var error = assertThrows(RestClientResponseException.class,
                    () -> analytics(client(tokens.generateToken("buyer")), "platform"));
            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        } finally { unavailable.set(false); }
    }

    @Test
    void rejectsMissingMalformedExpiredAndWrongSignatureJwt() {
        var expired = Jwts.builder().subject("buyer").expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        var foreign = Jwts.builder().subject("buyer").expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("another-secret-for-service-jwt-validation-at-least-64-bytes-long!!!".getBytes(StandardCharsets.UTF_8))).compact();
        for (String jwt : new String[] {null, "malformed", expired, foreign}) {
            var failure = assertThrows(RestClientResponseException.class,
                    () -> analytics(client(jwt), "platform"));
            assertEquals(HttpStatus.UNAUTHORIZED, failure.getStatusCode());
        }
    }
}
