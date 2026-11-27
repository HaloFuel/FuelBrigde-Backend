package com.halofuel.fuelbridge.platform.steps;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Stands in for the Ordering endpoints of the FuelBridge Platform in BDD tests: keeps fuel
 * orders in memory and implements GET /api/v1/fuel-orders/{id} and the dispatch and receive
 * transitions with the same state rules as the FuelOrder aggregate. It also records the
 * Authorization header it receives, to verify that Fulfillment relays the caller's token.
 */
public class OrderingHttpStub implements AutoCloseable {

    private static final Pattern ROUTE = Pattern.compile("/api/v1/fuel-orders/(\\d+)(/dispatch|/receive)?");

    private final HttpServer server;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<Long, StubOrder> orders = new ConcurrentHashMap<>();
    private final Set<Long> rejectedDispatches = ConcurrentHashMap.newKeySet();
    private final AtomicLong sequence = new AtomicLong(4000);
    private volatile String lastAuthorization;

    public OrderingHttpStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/fuel-orders", this::handle);
            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private synchronized void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            var matcher = ROUTE.matcher(exchange.getRequestURI().getPath());
            if (!matcher.matches()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var orderId = Long.parseLong(matcher.group(1));
            var order = orders.get(orderId);
            if (order == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var transition = matcher.group(2);
            if ("/dispatch".equals(transition)) {
                if (rejectedDispatches.contains(orderId) || !"PENDING".equals(order.status)) {
                    exchange.sendResponseHeaders(409, -1);
                    return;
                }
                order.status = "DISPATCHED";
            } else if ("/receive".equals(transition)) {
                if (!"DISPATCHED".equals(order.status)) {
                    exchange.sendResponseHeaders(409, -1);
                    return;
                }
                order.status = "PENDING_PAYMENT";
            }
            var body = mapper.writeValueAsString(order.toResource()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    /** Registers an order and returns its identifier. */
    public synchronized Long addOrder(long providerId, Long fuelProductId, Long equipmentId,
                                      double requestedQuantity, String status) {
        var id = sequence.incrementAndGet();
        orders.put(id, new StubOrder(id, providerId, fuelProductId, equipmentId, requestedQuantity, status));
        return id;
    }

    public synchronized void setStatus(Long orderId, String status) {
        orders.get(orderId).status = status;
    }

    public synchronized String statusOf(Long orderId) {
        return orders.get(orderId).status;
    }

    /** Makes Ordering answer 409 to the next dispatch requests of the order. */
    public void rejectDispatchOf(Long orderId) {
        rejectedDispatches.add(orderId);
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

    public record OrderResource(Long id, Long requestId, Long companyId, Long providerId, Long fuelProductId,
                                Long equipmentId, Double requestedQuantity, Double totalPrice, String status,
                                String deliveryAddress, String scheduledDate) {}

    private static final class StubOrder {
        private final Long id;
        private final long providerId;
        private final Long fuelProductId;
        private final Long equipmentId;
        private final double requestedQuantity;
        private String status;

        private StubOrder(Long id, long providerId, Long fuelProductId, Long equipmentId,
                          double requestedQuantity, String status) {
            this.id = id;
            this.providerId = providerId;
            this.fuelProductId = fuelProductId;
            this.equipmentId = equipmentId;
            this.requestedQuantity = requestedQuantity;
            this.status = status;
        }

        private OrderResource toResource() {
            return new OrderResource(id, null, 101L, providerId, fuelProductId, equipmentId, requestedQuantity,
                    requestedQuantity * 16.0, status, "Av. Separadora Industrial 2450, Villa El Salvador, Lima",
                    "2026-11-20");
        }
    }
}
