package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.InventoryServiceClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceGatewayTest {
    @Test
    void relaysFuelProductRoutesBodiesBearerAndBusinessErrors() throws Exception {
        var calls = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/fuel-products", exchange -> {
            calls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                    + exchange.getRequestHeaders().getFirst("Authorization") + " "
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var path = exchange.getRequestURI().getPath();
            int status = switch (exchange.getRequestMethod()) {
                case "DELETE" -> 204;
                case "POST" -> path.endsWith("/update-stock") ? 400 : 201;
                default -> path.endsWith("/999") ? 404 : 200;
            };
            if (status == 204) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                var response = "{\"code\":\"VALIDATION_ERROR\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, response.length);
                exchange.getResponseBody().write(response);
            }
            exchange.close();
        });
        server.start();
        try {
            var gateway = new InventoryServiceGateway(
                    new InventoryServiceClient("http://127.0.0.1:" + server.getAddress().getPort()));
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer signed-jwt");
            request.setContentType("application/json");

            assertEquals(HttpStatus.OK, gateway.getAll(request).getStatusCode());
            assertEquals(HttpStatus.OK, gateway.getByProvider(202L, request).getStatusCode());
            assertEquals(HttpStatus.NOT_FOUND, gateway.getById(999L, request).getStatusCode());
            var body = "{\"name\":\"Diesel B5\"}".getBytes(StandardCharsets.UTF_8);
            assertEquals(HttpStatus.CREATED, gateway.create(body, request).getStatusCode());
            var negative = gateway.updateStock(303L, "{\"newStock\":-100}".getBytes(StandardCharsets.UTF_8), request);
            assertEquals(HttpStatus.BAD_REQUEST, negative.getStatusCode());
            assertEquals("{\"code\":\"VALIDATION_ERROR\"}", new String(negative.getBody(), StandardCharsets.UTF_8));
            assertEquals(HttpStatus.OK, gateway.update(303L, body, request).getStatusCode());
            assertEquals(HttpStatus.NO_CONTENT, gateway.delete(303L, request).getStatusCode());

            assertTrue(calls.get(0).startsWith("GET /api/v1/fuel-products Bearer signed-jwt"));
            assertTrue(calls.get(1).startsWith("GET /api/v1/fuel-products/provider/202 "));
            assertTrue(calls.get(3).startsWith("POST /api/v1/fuel-products Bearer signed-jwt {\"name\":\"Diesel B5\"}"));
            assertTrue(calls.get(4).startsWith("POST /api/v1/fuel-products/303/update-stock "));
            assertTrue(calls.get(5).startsWith("PUT /api/v1/fuel-products/303 "));
            assertTrue(calls.get(6).startsWith("DELETE /api/v1/fuel-products/303 "));
        } finally {
            server.stop(0);
        }
    }
}
