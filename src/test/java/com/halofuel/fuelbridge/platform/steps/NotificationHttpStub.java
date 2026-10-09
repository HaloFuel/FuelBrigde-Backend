package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.infrastructure.notification.NotificationServiceClient.NotificationRequest;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Records requests accepted by the notification service boundary in BDD tests. */
public class NotificationHttpStub implements AutoCloseable {
    private final HttpServer server;
    private final List<AcceptedNotification> notifications = new CopyOnWriteArrayList<>();

    public NotificationHttpStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var mapper = JsonMapper.builder().build();
            server.createContext("/api/v1/notifications", exchange -> {
                if (!exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(405, -1);
                } else {
                    var request = mapper.readValue(exchange.getRequestBody().readAllBytes(), NotificationRequest.class);
                    notifications.add(new AcceptedNotification(request,
                            exchange.getRequestHeaders().getFirst("Authorization")));
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
    public void clear() { notifications.clear(); }
    public List<AcceptedNotification> accepted() { return List.copyOf(notifications); }
    @Override public void close() { server.stop(0); }

    public record AcceptedNotification(NotificationRequest request, String authorization) {}
}
