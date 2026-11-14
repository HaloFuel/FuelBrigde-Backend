package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.FulfillmentServiceClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FulfillmentServiceGatewayTest {

    @Test
    void relaysDeliveryDriverAndVehicleRoutesBodiesBearerAndBusinessErrors() throws Exception {
        List<String> calls = new ArrayList<>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1", exchange -> {
            calls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                    + exchange.getRequestHeaders().getFirst("Authorization") + " "
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var path = exchange.getRequestURI().getPath();
            int status;
            if (exchange.getRequestMethod().equals("DELETE")) {
                status = 204;
            } else if (path.endsWith("/complete")) {
                status = 409;
            } else if (exchange.getRequestMethod().equals("POST") && !path.contains("/deliveries/")) {
                status = 201;
            } else {
                status = 200;
            }
            if (status == 204) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            var response = (status == 409 ? "{\"code\":\"DELIVERY_CONFLICT\"}" : "{}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var gateway = new FulfillmentServiceGateway(
                    new FulfillmentServiceClient("http://127.0.0.1:" + server.getAddress().getPort()));
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer signed-jwt");
            request.setContentType("application/json");
            byte[] delivery = "{\"orderId\":4101}".getBytes(StandardCharsets.UTF_8);
            byte[] driver = "{\"licenseNumber\":\"Q45892176\"}".getBytes(StandardCharsets.UTF_8);

            assertEquals(HttpStatus.CREATED, gateway.createDelivery(delivery, request).getStatusCode());
            var conflict = gateway.completeDelivery(901L, request);
            assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
            assertEquals("{\"code\":\"DELIVERY_CONFLICT\"}", new String(conflict.getBody(), StandardCharsets.UTF_8));
            assertEquals(HttpStatus.OK, gateway.getDeliveryByOrder(4101L, request).getStatusCode());
            assertEquals(HttpStatus.OK, gateway.getDriversByProvider(202L, request).getStatusCode());
            assertEquals(HttpStatus.CREATED, gateway.createDriver(driver, request).getStatusCode());
            assertEquals(HttpStatus.OK, gateway.updateVehicle(701L, "{\"brand\":\"Volvo\"}".getBytes(StandardCharsets.UTF_8), request).getStatusCode());
            assertEquals(HttpStatus.NO_CONTENT, gateway.deleteVehicle(701L, request).getStatusCode());

            assertTrue(calls.get(0).startsWith("POST /api/v1/deliveries Bearer signed-jwt {\"orderId\":4101}"));
            assertTrue(calls.get(1).startsWith("POST /api/v1/deliveries/901/complete Bearer signed-jwt"));
            assertTrue(calls.get(2).startsWith("GET /api/v1/deliveries/order/4101 "));
            assertTrue(calls.get(3).startsWith("GET /api/v1/drivers?providerId=202 "));
            assertTrue(calls.get(4).endsWith("{\"licenseNumber\":\"Q45892176\"}"));
            assertTrue(calls.get(5).startsWith("PUT /api/v1/vehicles/701 Bearer signed-jwt {\"brand\":\"Volvo\"}"));
            assertTrue(calls.get(6).startsWith("DELETE /api/v1/vehicles/701 Bearer signed-jwt"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void answersBadGatewayWhenTheFulfillmentServiceIsDown() {
        var gateway = new FulfillmentServiceGateway(new FulfillmentServiceClient("http://127.0.0.1:1"));
        var request = new MockHttpServletRequest();

        assertEquals(HttpStatus.BAD_GATEWAY, gateway.getAllDeliveries(request).getStatusCode());
    }
}
