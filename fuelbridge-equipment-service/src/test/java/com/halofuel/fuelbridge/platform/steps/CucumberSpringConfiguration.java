package com.halofuel.fuelbridge.platform.steps;

import com.sun.net.httpserver.HttpServer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.io.IOException;
import java.net.InetSocketAddress;
import com.halofuel.fuelbridge.platform.FuelBridgeEquipmentServiceApplication;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the equipment service for Cucumber scenarios and stands in for IAM, which
 * Equipment asks whether a provider company exists before assigning it as favorite.
 * Every provider exists except id 99, matching the catalog service test stubs.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgeEquipmentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    private static final HttpServer IAM = startIam();

    private static HttpServer startIam() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/provider-companies/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                exchange.sendResponseHeaders(path.endsWith("/99") ? 404 : 200, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("fuelbridge.api.url", () -> "http://127.0.0.1:" + IAM.getAddress().getPort());
    }
}
