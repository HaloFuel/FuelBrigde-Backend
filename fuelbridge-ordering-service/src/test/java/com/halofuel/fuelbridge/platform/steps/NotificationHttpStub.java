package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.infrastructure.notification.NotificationServiceEventPublisher.NotificationRequest;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Records the requests accepted by the Notification service boundary in BDD tests; it can also simulate an outage. */
public class NotificationHttpStub implements AutoCloseable {
    private final HttpServer server;
    private final List<AcceptedNotification> notifications = new CopyOnWriteArrayList<>();
    private volatile boolean available = true;

    public NotificationHttpStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var mapper = JsonMapper.builder().build();
            server.createContext("/api/v1/notifications", exchange -> {
                if (!available) {
                    exchange.sendResponseHeaders(503, -1);
                } else if (!exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(405, -1);
                } else {
                    var request = mapper.readValue(exchange.getRequestBody().readAllBytes(), NotificationRequest.class);
                    notifications.add(new AcceptedNotification(request,
                            exchange.getRequestHeaders().getFirst("Authorization"),
                            exchange.getRequestHeaders().getFirst("Idempotency-Key")));
                    exchange.sendResponseHeaders(201, -1);
                }
                exchange.close();
            });
            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    public void clear() { notifications.clear(); available = true; }
    /** While unavailable the stand-in answers 503 to every request. */
    public void setAvailable(boolean available) { this.available = available; }
    public List<AcceptedNotification> accepted() { return List.copyOf(notifications); }
    @Override public void close() { server.stop(0); }

    public record AcceptedNotification(NotificationRequest request, String authorization, String idempotencyKey) {}
}
