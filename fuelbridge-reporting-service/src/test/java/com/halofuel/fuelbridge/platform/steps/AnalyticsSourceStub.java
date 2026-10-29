package com.halofuel.fuelbridge.platform.steps;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Embedded stand-in for Ordering/Payment/Fulfillment, the services reporting
 * reads from over HTTP. Mirrors the fixtures used by
 * {@code ReportingServiceIntegrationTest}: buyer company 7 has one CONFIRMED
 * order with a COMPLETED 100 payment and one PENDING order with a PENDING 30
 * payment. {@link #UNAVAILABLE} lets scenarios simulate an upstream outage.
 */
final class AnalyticsSourceStub {

    static final AtomicBoolean UNAVAILABLE = new AtomicBoolean(false);
    private static final HttpServer SERVER = start();

    private AnalyticsSourceStub() {
    }

    static String baseUrl() {
        return "http://127.0.0.1:" + SERVER.getAddress().getPort();
    }

    private static HttpServer start() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/", exchange -> {
                if (UNAVAILABLE.get()) {
                    exchange.sendResponseHeaders(503, -1);
                    exchange.close();
                    return;
                }
                String body = switch (exchange.getRequestURI().getPath()) {
                    case "/api/v1/fuel-orders/company/7" -> """
                            [{"id":1,"status":"CONFIRMED"},{"id":2,"status":"PENDING"}]""";
                    case "/api/v1/payments/company/7" -> """
                            [{"orderId":1,"amount":100,"status":"COMPLETED","paidAt":"2026-01-15T10:00:00"},
                             {"orderId":2,"amount":30,"status":"PENDING","paidAt":null}]""";
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
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
