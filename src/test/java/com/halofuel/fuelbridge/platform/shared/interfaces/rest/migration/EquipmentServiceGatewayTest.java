package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.EquipmentServiceClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentServiceGatewayTest {
    @Test
    void relaysEquipmentRoutesBodiesBearerAndBusinessErrors() throws Exception {
        var calls = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/equipment", exchange -> {
            calls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                    + exchange.getRequestHeaders().getFirst("Authorization") + " "
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var path = exchange.getRequestURI().getPath();
            var status = exchange.getRequestMethod().equals("DELETE") ? 204
                    : path.endsWith("/999/fuel-receipts") ? 404 : path.endsWith("/fuel-receipts") ? 400 : 200;
            var response = "{\"code\":\"VALIDATION_ERROR\"}".getBytes(StandardCharsets.UTF_8);
            if (status == 204) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var gateway = new EquipmentServiceGateway(
                    new EquipmentServiceClient("http://127.0.0.1:" + server.getAddress().getPort()));
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer signed-jwt");
            request.setContentType("application/json");
            byte[] body = "{\"quantity\":0}".getBytes(StandardCharsets.UTF_8);

            assertEquals(HttpStatus.OK, gateway.getAll(request).getStatusCode());
            assertEquals(HttpStatus.OK, gateway.getById(305L, request).getStatusCode());
            assertEquals(HttpStatus.OK, gateway.getByCompany(101L, request).getStatusCode());
            assertEquals(HttpStatus.OK, gateway.update(305L, "{\"name\":\"CAT\"}".getBytes(StandardCharsets.UTF_8), request).getStatusCode());
            var invalid = gateway.receiveFuel(305L, body, request);
            assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
            assertEquals("{\"code\":\"VALIDATION_ERROR\"}", new String(invalid.getBody(), StandardCharsets.UTF_8));
            assertEquals(HttpStatus.NOT_FOUND, gateway.receiveFuel(999L, body, request).getStatusCode());
            assertEquals(HttpStatus.NO_CONTENT, gateway.delete(305L, request).getStatusCode());

            assertTrue(calls.get(0).startsWith("GET /api/v1/equipment Bearer signed-jwt"));
            assertTrue(calls.get(1).startsWith("GET /api/v1/equipment/305 "));
            assertTrue(calls.get(2).startsWith("GET /api/v1/equipment/company/101 "));
            assertTrue(calls.get(3).startsWith("POST /api/v1/equipment/305/update Bearer signed-jwt {\"name\":\"CAT\"}"));
            assertTrue(calls.get(4).endsWith("{\"quantity\":0}"));
            assertTrue(calls.get(6).startsWith("DELETE /api/v1/equipment/305 Bearer signed-jwt"));
        } finally {
            server.stop(0);
        }
    }
}
