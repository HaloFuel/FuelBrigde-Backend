package com.halofuel.fuelbridge.platform;

import com.halofuel.fuelbridge.platform.equipment.infrastructure.persistence.jpa.repositories.EquipmentPersistenceRepository;
import com.halofuel.fuelbridge.platform.equipment.infrastructure.security.tokens.jwt.BearerTokenService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EquipmentServiceIntegrationTest {
    private static final String SECRET = "test-secret-for-service-jwt-validation-at-least-64-bytes-long!!!!!!";
    @LocalServerPort private int port;
    @Autowired private BearerTokenService tokens;
    @Autowired private EquipmentPersistenceRepository repository;

    @BeforeEach void clearEquipment() { repository.deleteAll(); }

    private RestClient client(String jwt) {
        var builder = RestClient.builder().baseUrl("http://127.0.0.1:" + port);
        if (jwt != null) builder.defaultHeader("Authorization", "Bearer " + jwt);
        return builder.build();
    }

    private static Map<String, Object> equipment(String name, long companyId) {
        var body = new LinkedHashMap<String, Object>();
        body.put("name", name);
        body.put("equipmentType", "EXCAVATOR");
        body.put("licensePlate", "EQ-320-LIM");
        body.put("fuelType", "DIESEL");
        body.put("tankCapacity", 400);
        body.put("currentLevel", 80);
        body.put("location", "Obra Villa El Salvador, Lima");
        body.put("status", "ACTIVE");
        body.put("autoRefill", true);
        body.put("refillThreshold", 25);
        body.put("lastRefillDate", "2026-10-08");
        body.put("companyId", companyId);
        return body;
    }

    private Map<?, ?> create(RestClient client, String name, long companyId) {
        var response = client.post().uri("/api/v1/equipment").contentType(MediaType.APPLICATION_JSON)
                .body(equipment(name, companyId)).retrieve().toEntity(Map.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private static long id(Map<?, ?> body) { return ((Number) body.get("id")).longValue(); }

    @Test
    void registersUpdatesAssignsProviderAndListsEquipmentByCompany() {
        var client = client(tokens.generateToken("buyer"));
        var excavator = create(client, "Excavadora CAT 320", 101);
        create(client, "Generador Cummins", 101);
        create(client, "Montacargas Toyota", 102);

        var update = new HashMap<>(equipment("Excavadora CAT 320 renovada", 101));
        update.remove("companyId");
        var updated = client.post().uri("/api/v1/equipment/{id}/update", id(excavator))
                .contentType(MediaType.APPLICATION_JSON).body(update).retrieve().body(Map.class);
        assertEquals("Excavadora CAT 320 renovada", updated.get("name"));
        assertEquals(101, ((Number) updated.get("companyId")).intValue());

        var withProvider = client.post().uri("/api/v1/equipment/{id}/favorite-provider", id(excavator))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("providerId", 202))
                .retrieve().body(Map.class);
        assertEquals(202, ((Number) withProvider.get("favoriteProviderId")).intValue());

        var ofCompany = client.get().uri("/api/v1/equipment/company/101").retrieve().body(Map[].class);
        assertEquals(2, ofCompany.length);
        assertEquals(3, client.get().uri("/api/v1/equipment").retrieve().body(Map[].class).length);
    }

    @Test
    void receivedFuelIsAddedWithoutExceedingTankCapacity() {
        var client = client(tokens.generateToken("fuelbridge-fulfillment"));
        var created = create(client, "Excavadora CAT 320", 101);

        var refilled = client.post().uri("/api/v1/equipment/{id}/fuel-receipts", id(created))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", 240))
                .retrieve().body(Map.class);
        assertEquals(320, ((Number) refilled.get("currentLevel")).intValue());

        var full = client.post().uri("/api/v1/equipment/{id}/fuel-receipts", id(created))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", 500))
                .retrieve().body(Map.class);
        assertEquals(400, ((Number) full.get("currentLevel")).intValue());
    }

    @Test
    void rejectsInvalidQuantitiesAndUnknownEquipment() {
        var client = client(tokens.generateToken("buyer"));
        var created = create(client, "Excavadora CAT 320", 101);
        for (var quantity : new int[] {0, -5}) {
            var invalid = assertThrows(RestClientResponseException.class, () -> client.post()
                    .uri("/api/v1/equipment/{id}/fuel-receipts", id(created))
                    .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", quantity))
                    .retrieve().toBodilessEntity());
            assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
        }
        var unknownReceipt = assertThrows(RestClientResponseException.class, () -> client.post()
                .uri("/api/v1/equipment/999999/fuel-receipts")
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", 100))
                .retrieve().toBodilessEntity());
        assertEquals(HttpStatus.NOT_FOUND, unknownReceipt.getStatusCode());
        var unknownGet = assertThrows(RestClientResponseException.class, () -> client.get()
                .uri("/api/v1/equipment/999999").retrieve().toBodilessEntity());
        assertEquals(HttpStatus.NOT_FOUND, unknownGet.getStatusCode());

        var stored = client.get().uri("/api/v1/equipment/{id}", id(created)).retrieve().body(Map.class);
        assertEquals(80, ((Number) stored.get("currentLevel")).intValue());
    }

    @Test
    void rejectsMissingMalformedExpiredAndWrongSignatureJwt() {
        var expired = Jwts.builder().subject("buyer").expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        var foreign = Jwts.builder().subject("buyer").expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("another-secret-for-service-jwt-validation-at-least-64-bytes-long!!!".getBytes(StandardCharsets.UTF_8))).compact();
        for (String jwt : new String[] {null, "malformed", expired, foreign}) {
            var failure = assertThrows(RestClientResponseException.class, () -> client(jwt).get()
                    .uri("/api/v1/equipment").retrieve().toBodilessEntity());
            assertEquals(HttpStatus.UNAUTHORIZED, failure.getStatusCode());
        }
    }
}
