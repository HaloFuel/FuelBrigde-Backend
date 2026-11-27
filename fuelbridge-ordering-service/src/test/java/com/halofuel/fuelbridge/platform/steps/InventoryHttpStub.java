package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.application.outboundservices.FuelProductCatalog.FuelProductSnapshot;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Stands in for GET /api/v1/fuel-products/{id} of the Inventory service in BDD tests and
 * records the Authorization header it receives.
 */
public class InventoryHttpStub implements AutoCloseable {

    private static final Pattern ROUTE = Pattern.compile("/api/v1/fuel-products/(\\d+)");

    private final HttpServer server;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<Long, FuelProductSnapshot> products = new ConcurrentHashMap<>();
    private volatile String lastAuthorization;

    public InventoryHttpStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/fuel-products", this::handle);
            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            var matcher = ROUTE.matcher(exchange.getRequestURI().getPath());
            var product = matcher.matches() ? products.get(Long.parseLong(matcher.group(1))) : null;
            if (product == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var body = mapper.writeValueAsString(product).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    public void addProduct(Long productId, Long providerId, double pricePerUnit) {
        products.put(productId, new FuelProductSnapshot(productId, "Diesel B5 S-50", "DIESEL", pricePerUnit,
                "L", 50000.0, 60000.0, providerId, true));
    }

    public void reset() {
        products.clear();
        lastAuthorization = null;
    }

    public String lastAuthorization() {
        return lastAuthorization;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
