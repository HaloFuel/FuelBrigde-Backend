package com.halofuel.fuelbridge.platform;

import com.halofuel.fuelbridge.platform.fulfillment.infrastructure.persistence.jpa.repositories.DeliveryPersistenceRepository;
import com.halofuel.fuelbridge.platform.fulfillment.infrastructure.persistence.jpa.repositories.DriverPersistenceRepository;
import com.halofuel.fuelbridge.platform.fulfillment.infrastructure.persistence.jpa.repositories.VehiclePersistenceRepository;
import com.halofuel.fuelbridge.platform.fulfillment.infrastructure.security.tokens.jwt.BearerTokenService;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the service over HTTP: JWT validation, fleet registration and delivery queries. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FulfillmentServiceIntegrationTest {

    @LocalServerPort private int port;
    @Autowired private BearerTokenService tokens;
    @Autowired private DeliveryPersistenceRepository deliveries;
    @Autowired private DriverPersistenceRepository drivers;
    @Autowired private VehiclePersistenceRepository vehicles;

    @BeforeEach
    void clearData() {
        deliveries.deleteAll();
        drivers.deleteAll();
        vehicles.deleteAll();
    }

    private RestClient client(String jwt) {
        var builder = RestClient.builder().baseUrl("http://127.0.0.1:" + port);
        if (jwt != null) builder.defaultHeader("Authorization", "Bearer " + jwt);
        return builder.build();
    }

    private static int statusOf(Runnable call) {
        try {
            call.run();
            return 200;
        } catch (RestClientResponseException ex) {
            return ex.getStatusCode().value();
        }
    }

    @Test
    void requestsWithoutAValidTokenAreRejected() {
        assertEquals(401, statusOf(() -> client(null).get().uri("/api/v1/deliveries").retrieve().toBodilessEntity()));
        assertEquals(401, statusOf(() -> client("not-a-jwt").get().uri("/api/v1/vehicles?providerId=202")
                .retrieve().toBodilessEntity()));
    }

    @Test
    void registersVehiclesAndDriversAndListsThemByProvider() {
        var client = client(tokens.generateToken("provider.andina"));

        var vehicle = client.post().uri("/api/v1/vehicles").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("providerId", 202, "licensePlate", "BFG-482", "brand", "Volvo", "model", "FM 460",
                        "capacity", 12000, "unit", "L", "status", "AVAILABLE"))
                .retrieve().toEntity(Map.class);
        assertEquals(HttpStatus.CREATED, vehicle.getStatusCode());

        var driver = client.post().uri("/api/v1/drivers").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("providerId", 202, "firstName", "Carlos", "lastName", "Quispe",
                        "licenseNumber", "Q45892176", "phoneNumber", "+51987654321",
                        "email", "carlos.quispe@example.com", "status", "AVAILABLE"))
                .retrieve().toEntity(Map.class);
        assertEquals(HttpStatus.CREATED, driver.getStatusCode());

        List<?> fleet = client.get().uri("/api/v1/vehicles?providerId=202").retrieve().body(List.class);
        List<?> otherProvider = client.get().uri("/api/v1/vehicles?providerId=303").retrieve().body(List.class);
        assertEquals(1, fleet.size());
        assertTrue(otherProvider.isEmpty());
    }

    @Test
    void deliveryQueriesAnswerWithTheStoredDeliveries() {
        var client = client(tokens.generateToken("provider.andina"));

        List<?> all = client.get().uri("/api/v1/deliveries").retrieve().body(List.class);
        assertTrue(all.isEmpty());
        assertEquals(404, statusOf(() -> client.get().uri("/api/v1/deliveries/999999")
                .retrieve().toBodilessEntity()));
    }
}
