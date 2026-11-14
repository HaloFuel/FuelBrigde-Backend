package com.halofuel.fuelbridge.platform.steps;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Stands in for the standalone Equipment service in BDD tests: implements
 * POST /api/v1/equipment/{id}/fuel-receipts and records the fuel each equipment received.
 */
public class EquipmentHttpStub implements AutoCloseable {

    private static final Pattern ROUTE = Pattern.compile("/api/v1/equipment/(\\d+)/fuel-receipts");

    private final HttpServer server;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<Long, Double> received = new ConcurrentHashMap<>();
    private volatile boolean available = true;

    public EquipmentHttpStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/equipment", this::handle);
            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private synchronized void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!available) {
                exchange.sendResponseHeaders(503, -1);
                return;
            }
            var matcher = ROUTE.matcher(exchange.getRequestURI().getPath());
            if (!matcher.matches()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var equipmentId = Long.parseLong(matcher.group(1));
            if (!received.containsKey(equipmentId)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var quantity = mapper.readValue(exchange.getRequestBody().readAllBytes(), QuantityRequest.class).quantity();
            received.merge(equipmentId, quantity, Double::sum);
            exchange.sendResponseHeaders(200, -1);
        }
    }

    /** Registers an equipment that has not received fuel yet. */
    public void addEquipment(Long equipmentId) {
        received.put(equipmentId, 0.0);
    }

    public double receivedBy(Long equipmentId) {
        return received.getOrDefault(equipmentId, 0.0);
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void reset() {
        received.clear();
        available = true;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public record QuantityRequest(Double quantity) {}
}
