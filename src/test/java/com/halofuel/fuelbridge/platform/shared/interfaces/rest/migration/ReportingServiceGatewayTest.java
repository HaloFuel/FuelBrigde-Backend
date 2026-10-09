package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.ReportingServiceClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ReportingServiceGatewayTest {
    @Test
    void forwardsAllAnalyticsRoutesAndBearer() throws Exception {
        var calls = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/analytics", exchange -> {
            calls.add(exchange.getRequestURI() + " " + exchange.getRequestHeaders().getFirst("Authorization"));
            var response = "{\"totalOrders\":3}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        var gateway = new ReportingServiceGateway(new ReportingServiceClient("http://127.0.0.1:" + server.getAddress().getPort()));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer signed-jwt");
        try {
            var response = gateway.platform(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("{\"totalOrders\":3}", new String(response.getBody(), StandardCharsets.UTF_8));
            gateway.provider(9L, request);
            gateway.buyer(7L, request);
            assertEquals(java.util.List.of("/api/v1/analytics/platform Bearer signed-jwt",
                    "/api/v1/analytics/providers/9 Bearer signed-jwt", "/api/v1/analytics/buyers/7 Bearer signed-jwt"), calls);
        } finally {
            server.stop(0);
        }
        var failure = gateway.platform(request);
        assertEquals(HttpStatus.BAD_GATEWAY, failure.getStatusCode());
    }
}
