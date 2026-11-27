package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.CatalogServiceClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CatalogServiceGatewayTest {
    @Test
    void relaysRatingsRoutesFiltersBodiesBearerAndBusinessErrors() throws Exception {
        var calls = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/provider-ratings", exchange -> {
            calls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                    + exchange.getRequestHeaders().getFirst("Authorization") + " "
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var response = "{\"message\":\"duplicate\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(exchange.getRequestMethod().equals("POST") ? 409 : 200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var gateway = new CatalogServiceGateway(new CatalogServiceClient("http://127.0.0.1:" + server.getAddress().getPort()));
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer signed-jwt");
            request.setContentType("application/json");
            var query = new LinkedMultiValueMap<String, String>();
            query.add("companyId", "7");
            query.add("providerId", "9");
            assertEquals(HttpStatus.OK, gateway.getAll(query, request).getStatusCode());
            byte[] body = "{\"companyId\":7,\"providerId\":9,\"rating\":5}".getBytes(StandardCharsets.UTF_8);
            var created = gateway.create(body, request);
            assertEquals(HttpStatus.CONFLICT, created.getStatusCode());
            assertEquals("application/json", created.getHeaders().getContentType().toString());
            assertEquals("{\"message\":\"duplicate\"}", new String(created.getBody(), StandardCharsets.UTF_8));
            assertEquals(HttpStatus.OK, gateway.update(42L, body, request).getStatusCode());
            assertTrue(calls.get(0).contains("companyId=7&providerId=9 Bearer signed-jwt"));
            assertTrue(calls.get(1).endsWith(new String(body, StandardCharsets.UTF_8)));
            assertTrue(calls.get(2).startsWith("PUT /api/v1/provider-ratings/42 Bearer signed-jwt"));
        } finally {
            server.stop(0);
        }
    }
}
