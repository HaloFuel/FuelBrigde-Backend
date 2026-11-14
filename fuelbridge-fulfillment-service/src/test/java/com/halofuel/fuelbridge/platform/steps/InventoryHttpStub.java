package com.halofuel.fuelbridge.platform.steps;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Stands in for the standalone Inventory service in BDD tests: keeps fuel products in memory
 * and implements the stock-reservations and stock-releases contract used by Fulfillment.
 */
public class InventoryHttpStub implements AutoCloseable {
    private static final Pattern ROUTE = Pattern.compile("/api/v1/fuel-products/(\\d+)(/stock-reservations|/stock-releases)?");
    private final HttpServer server;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<Long, StubProduct> products = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    public InventoryHttpStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/fuel-products", this::handle);
            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private synchronized void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            var matcher = ROUTE.matcher(exchange.getRequestURI().getPath());
            if (!matcher.matches()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var product = products.get(Long.parseLong(matcher.group(1)));
            if (product == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var operation = matcher.group(2);
            if (operation != null) {
                var quantity = mapper.readValue(exchange.getRequestBody().readAllBytes(), QuantityRequest.class).quantity();
                if (operation.equals("/stock-reservations")) {
                    if (product.availableStock < quantity) {
                        exchange.sendResponseHeaders(409, -1);
                        return;
                    }
                    product.availableStock -= quantity;
                } else {
                    product.availableStock += quantity;
                }
            }
            var body = mapper.writeValueAsString(product.toResource()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    /** Registers a product and returns its identifier. */
    public synchronized Long addProduct(String name, double pricePerUnit, double availableStock, long providerId) {
        var id = sequence.incrementAndGet();
        products.put(id, new StubProduct(id, name, pricePerUnit, availableStock, providerId));
        return id;
    }

    public synchronized void setStock(Long productId, double availableStock) {
        products.get(productId).availableStock = availableStock;
    }

    public synchronized double stockOf(Long productId) {
        return products.get(productId).availableStock;
    }

    public String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    @Override public void close() { server.stop(0); }

    public record QuantityRequest(Double quantity) {}

    public record ProductResource(Long id, String name, String fuelType, Double pricePerUnit, String unit,
                                  Double availableStock, Double capacity, Long providerId, Boolean active) {}

    private static final class StubProduct {
        private final Long id;
        private final String name;
        private final double pricePerUnit;
        private final long providerId;
        private double availableStock;

        private StubProduct(Long id, String name, double pricePerUnit, double availableStock, long providerId) {
            this.id = id;
            this.name = name;
            this.pricePerUnit = pricePerUnit;
            this.availableStock = availableStock;
            this.providerId = providerId;
        }

        private ProductResource toResource() {
            return new ProductResource(id, name, "DIESEL", pricePerUnit, "GAL", availableStock, 20000.0, providerId, true);
        }
    }
}
