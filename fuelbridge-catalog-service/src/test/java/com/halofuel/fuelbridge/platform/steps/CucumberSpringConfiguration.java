package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgeCatalogServiceApplication;
import com.sun.net.httpserver.HttpServer;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Boots the catalog service for Cucumber scenarios and stands in for IAM,
 * the only external dependency provider-rating validation calls over HTTP.
 * Any identifier is treated as an existing company, except "/99" which IAM
 * reports as not found — matching {@code CatalogServiceIntegrationTest}.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgeCatalogServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    private static final HttpServer IAM = startIam();

    private static HttpServer startIam() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/", exchange -> {
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
