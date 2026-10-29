package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgeNotificationServiceApplication;
import com.sun.net.httpserver.HttpServer;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Boots the notification service for Cucumber scenarios and stands in for
 * IAM's user directory, the only external dependency recipient resolution
 * calls over HTTP. Mirrors {@code NotificationServiceIntegrationTest}:
 * buyer company 101 resolves to user 1101.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgeNotificationServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    private static final HttpServer DIRECTORY = startDirectory();

    private static HttpServer startDirectory() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/users", exchange -> {
                byte[] body = "[{\"id\":1101,\"companyId\":101,\"providerId\":null}]"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
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
        registry.add("iam.service.url", () -> "http://127.0.0.1:" + DIRECTORY.getAddress().getPort());
    }
}
