package com.halofuel.fuelbridge.platform.steps;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * In-memory stand-in for the Ordering endpoints that Payment calls over HTTP.
 * POST /api/v1/fuel-orders/{orderId}/mark-paid answers 200, except for order
 * {@link #MISSING_ORDER} (404) and order {@link #REJECTING_ORDER} (409), and records
 * every order it was asked to mark as paid.
 */
final class OrderingHttpStub {

    static final long MISSING_ORDER = 9404L;
    static final long REJECTING_ORDER = 9409L;

    private static final Pattern MARK_PAID = Pattern.compile("/api/v1/fuel-orders/(\\d+)/mark-paid");
    private static final List<Long> MARKED_AS_PAID = new CopyOnWriteArrayList<>();
    private static final HttpServer SERVER = start();

    private OrderingHttpStub() {
    }

    static String baseUrl() {
        return "http://127.0.0.1:" + SERVER.getAddress().getPort();
    }

    static List<Long> ordersMarkedAsPaid() {
        return List.copyOf(MARKED_AS_PAID);
    }

    static void reset() {
        MARKED_AS_PAID.clear();
    }

    private static HttpServer start() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/fuel-orders/", exchange -> {
                var matcher = MARK_PAID.matcher(exchange.getRequestURI().getPath());
                int status;
                if (!"POST".equals(exchange.getRequestMethod()) || !matcher.matches()) {
                    status = 404;
                } else {
                    long orderId = Long.parseLong(matcher.group(1));
                    status = orderId == MISSING_ORDER ? 404 : orderId == REJECTING_ORDER ? 409 : 200;
                    if (status == 200) MARKED_AS_PAID.add(orderId);
                }
                exchange.sendResponseHeaders(status, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
