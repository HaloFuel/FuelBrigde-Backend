package com.halofuel.fuelbridge.platform;

import com.halofuel.fuelbridge.platform.catalog.infrastructure.persistence.jpa.repositories.ProviderRatingPersistenceRepository;
import com.halofuel.fuelbridge.platform.catalog.infrastructure.security.tokens.jwt.BearerTokenService;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CatalogServiceIntegrationTest {
    private static final String SECRET = "test-secret-for-service-jwt-validation-at-least-64-bytes-long!!!!!!";
    private static final AtomicReference<String> authorization = new AtomicReference<>();
    private static final HttpServer iam = startIam();
    @LocalServerPort private int port;
    @Autowired private BearerTokenService tokens;
    @Autowired private ProviderRatingPersistenceRepository repository;

    private static HttpServer startIam() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/", exchange -> {
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                String path = exchange.getRequestURI().getPath();
                exchange.sendResponseHeaders(path.endsWith("/99") ? 404 : path.endsWith("/98") ? 503 : 200, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("fuelbridge.api.url", () -> "http://127.0.0.1:" + iam.getAddress().getPort());
    }

    @AfterAll static void stopIam() { iam.stop(0); }
    @BeforeEach void clearRatings() { repository.deleteAll(); }

    private RestClient client(String jwt) {
        var builder = RestClient.builder().baseUrl("http://127.0.0.1:" + port);
        if (jwt != null) builder.defaultHeader("Authorization", "Bearer " + jwt);
        return builder.build();
    }

    private Map<?, ?> create(RestClient client, int buyer, int provider, int rating) {
        var response = client.post().uri("/api/v1/provider-ratings").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("companyId", buyer, "providerId", provider, "rating", rating))
                .retrieve().toEntity(Map.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    @Test
    void persistsUpdatesAndFiltersRatingsUsingSignedJwtAndRemoteIam() {
        var jwt = Jwts.builder().subject("buyer").issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        var client = client(jwt);
        var created = create(client, 7, 9, 1);
        assertEquals(1, ((Number) created.get("rating")).intValue());
        assertEquals("Bearer " + jwt, authorization.get());
        var updated = client.put().uri("/api/v1/provider-ratings/" + created.get("id"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("companyId", 7, "providerId", 9, "rating", 5)).retrieve().body(Map.class);
        assertEquals(5, ((Number) updated.get("rating")).intValue());
        create(client, 8, 10, 3);
        var filtered = client.get().uri("/api/v1/provider-ratings?companyId=7&providerId=9")
                .retrieve().body(Map[].class);
        assertEquals(1, filtered.length);
        assertEquals(5, ((Number) filtered[0].get("rating")).intValue());
        assertEquals(2, repository.count());
    }

    @Test
    void rejectsOutOfRangeDuplicateAndChangedRecipients() {
        var client = client(tokens.generateToken("buyer"));
        for (var rating : new int[] {0, 6}) {
            var error = assertThrows(RestClientResponseException.class, () -> create(client, 7, 9, rating));
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }
        var created = create(client, 7, 9, 5);
        var duplicate = assertThrows(RestClientResponseException.class, () -> create(client, 7, 9, 4));
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        var changed = assertThrows(RestClientResponseException.class, () -> client.put()
                .uri("/api/v1/provider-ratings/" + created.get("id")).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("companyId", 8, "providerId", 9, "rating", 3)).retrieve().toBodilessEntity());
        assertEquals(HttpStatus.BAD_REQUEST, changed.getStatusCode());
        assertEquals(1, repository.count());
    }

    @Test
    void distinguishesMissingCompaniesFromIamFailure() {
        var client = client(tokens.generateToken("buyer"));
        var missing = assertThrows(RestClientResponseException.class, () -> create(client, 99, 9, 5));
        assertEquals(HttpStatus.BAD_REQUEST, missing.getStatusCode());
        var unavailable = assertThrows(RestClientResponseException.class, () -> create(client, 98, 9, 5));
        assertEquals(HttpStatus.BAD_GATEWAY, unavailable.getStatusCode());
        assertEquals(0, repository.count());
    }

    @Test
    void rejectsMissingMalformedExpiredAndWrongSignatureJwt() {
        var expired = Jwts.builder().subject("buyer").expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        var foreign = Jwts.builder().subject("buyer").expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("another-secret-for-service-jwt-validation-at-least-64-bytes-long!!!".getBytes(StandardCharsets.UTF_8))).compact();
        for (String jwt : new String[] {null, "malformed", expired, foreign}) {
            var failure = assertThrows(RestClientResponseException.class, () -> client(jwt).get()
                    .uri("/api/v1/provider-ratings").retrieve().toBodilessEntity());
            assertEquals(HttpStatus.UNAUTHORIZED, failure.getStatusCode());
        }
    }
}
