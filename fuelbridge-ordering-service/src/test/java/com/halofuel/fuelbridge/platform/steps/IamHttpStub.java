package com.halofuel.fuelbridge.platform.steps;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Stands in for GET /api/v1/users/company/{companyId} of Identity &amp; Access in BDD tests:
 * answers the company's user, or 404 when the company has none.
 */
public class IamHttpStub implements AutoCloseable {

    private static final Pattern ROUTE = Pattern.compile("/api/v1/users/company/(\\d+)");

    private final HttpServer server;
    private final Map<Long, Long> userIdByCompany = new ConcurrentHashMap<>();

    public IamHttpStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/users", this::handle);
            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            var matcher = ROUTE.matcher(exchange.getRequestURI().getPath());
            var userId = matcher.matches() ? userIdByCompany.get(Long.parseLong(matcher.group(1))) : null;
            if (userId == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var body = "{\"id\":%d,\"username\":\"buyer-%d\"}".formatted(userId, userId)
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    public void registerUser(Long companyId, Long userId) {
        userIdByCompany.put(companyId, userId);
    }

    public void reset() {
        userIdByCompany.clear();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
