package com.halofuel.fuelbridge.platform;

import com.halofuel.fuelbridge.platform.inventory.infrastructure.persistence.jpa.repositories.FuelProductPersistenceRepository;
import com.halofuel.fuelbridge.platform.inventory.infrastructure.security.tokens.jwt.BearerTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InventoryServiceIntegrationTest {
    private static final String SECRET = "test-secret-for-service-jwt-validation-at-least-64-bytes-long!!!!!!";
    @LocalServerPort private int port;
    @Autowired private BearerTokenService tokens;
    @Autowired private FuelProductPersistenceRepository repository;

    @BeforeEach void clearProducts() { repository.deleteAll(); }

    private RestClient client(String jwt) {
        var builder = RestClient.builder().baseUrl("http://127.0.0.1:" + port);
        if (jwt != null) builder.defaultHeader("Authorization", "Bearer " + jwt);
        return builder.build();
    }

    private static Map<String, Object> product(String name, long providerId, double stock) {
        var body = new LinkedHashMap<String, Object>();
        body.put("name", name);
        body.put("fuelType", "DIESEL");
        body.put("pricePerUnit", 16.5);
        body.put("unit", "GAL");
        body.put("availableStock", stock);
        body.put("capacity", 20000);
        body.put("providerId", providerId);
        body.put("active", true);
        return body;
    }

    private Map<?, ?> create(RestClient client, String name, long providerId, double stock) {
        var response = client.post().uri("/api/v1/fuel-products").contentType(MediaType.APPLICATION_JSON)
                .body(product(name, providerId, stock)).retrieve().toEntity(Map.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private static long id(Map<?, ?> body) { return ((Number) body.get("id")).longValue(); }

    private static double stock(Map<?, ?> body) { return ((Number) body.get("availableStock")).doubleValue(); }

    @Test
    void createsUpdatesFiltersAndDeletesProductsWithSignedJwt() {
        var client = client(tokens.generateToken("provider"));
        var diesel = create(client, "Diésel B5", 202, 1000);
        create(client, "Gasohol 95", 202, 500);
        create(client, "GLP", 303, 800);

        assertEquals(2, client.get().uri("/api/v1/fuel-products/provider/202").retrieve().body(Map[].class).length);
        assertEquals(3, client.get().uri("/api/v1/fuel-products").retrieve().body(Map[].class).length);

        var restocked = client.post().uri("/api/v1/fuel-products/{id}/update-stock", id(diesel))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("newStock", 1500))
                .retrieve().body(Map.class);
        assertEquals(1500.0, stock(restocked));

        var deleted = client.delete().uri("/api/v1/fuel-products/{id}", id(diesel)).retrieve().toBodilessEntity();
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        assertEquals(2, repository.count());
    }

    @Test
    void reservesAndReleasesStockForFulfillment() {
        var client = client(tokens.generateToken("fuelbridge-fulfillment"));
        var created = create(client, "Diésel B5", 202, 1000);

        var reserved = client.post().uri("/api/v1/fuel-products/{id}/stock-reservations", id(created))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", 300))
                .retrieve().body(Map.class);
        assertEquals(700.0, stock(reserved));

        var insufficient = assertThrows(RestClientResponseException.class, () -> client.post()
                .uri("/api/v1/fuel-products/{id}/stock-reservations", id(created))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", 800))
                .retrieve().toBodilessEntity());
        assertEquals(HttpStatus.CONFLICT, insufficient.getStatusCode());

        var released = client.post().uri("/api/v1/fuel-products/{id}/stock-releases", id(created))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", 300))
                .retrieve().body(Map.class);
        assertEquals(1000.0, stock(released));
    }

    @Test
    void rejectsInvalidQuantitiesNegativeStockAndUnknownProducts() {
        var client = client(tokens.generateToken("provider"));
        var created = create(client, "Diésel B5", 202, 1000);
        for (var quantity : new int[] {0, -5}) {
            var invalid = assertThrows(RestClientResponseException.class, () -> client.post()
                    .uri("/api/v1/fuel-products/{id}/stock-reservations", id(created))
                    .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", quantity))
                    .retrieve().toBodilessEntity());
            assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
        }
        var negativeStock = assertThrows(RestClientResponseException.class, () -> client.post()
                .uri("/api/v1/fuel-products/{id}/update-stock", id(created))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("newStock", -100))
                .retrieve().toBodilessEntity());
        assertEquals(HttpStatus.BAD_REQUEST, negativeStock.getStatusCode());
        var unknown = assertThrows(RestClientResponseException.class, () -> client.post()
                .uri("/api/v1/fuel-products/999999/stock-reservations")
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", 100))
                .retrieve().toBodilessEntity());
        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());

        var stored = client.get().uri("/api/v1/fuel-products/{id}", id(created)).retrieve().body(Map.class);
        assertEquals(1000.0, stock(stored));
    }

    @Test
    void rejectsMissingMalformedExpiredAndWrongSignatureJwt() {
        var expired = Jwts.builder().subject("provider").expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        var foreign = Jwts.builder().subject("provider").expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("another-secret-for-service-jwt-validation-at-least-64-bytes-long!!!".getBytes(StandardCharsets.UTF_8))).compact();
        for (String jwt : new String[] {null, "malformed", expired, foreign}) {
            var failure = assertThrows(RestClientResponseException.class, () -> client(jwt).get()
                    .uri("/api/v1/fuel-products").retrieve().toBodilessEntity());
            assertEquals(HttpStatus.UNAUTHORIZED, failure.getStatusCode());
        }
    }
}
