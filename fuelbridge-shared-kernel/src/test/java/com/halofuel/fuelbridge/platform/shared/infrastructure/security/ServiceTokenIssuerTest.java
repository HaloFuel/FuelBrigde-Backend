package com.halofuel.fuelbridge.platform.shared.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTokenIssuerTest {

    private static final String SECRET = "test-secret-for-service-jwt-validation-at-least-64-bytes-long!!!!!!";

    @Test
    void issuedTokenIsValidatedWithTheSharedSecret() {
        var token = new ServiceTokenIssuer(SECRET).issueFor("fuelbridge-payment-service");

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("fuelbridge-payment-service", claims.getSubject());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void tokenExpiresAfterTheConfiguredTimeToLive() {
        var token = new ServiceTokenIssuer(SECRET, Duration.ofMinutes(2)).issueFor("fuelbridge-fulfillment-service");

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long lifetimeMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(Duration.ofMinutes(2).toMillis(), lifetimeMillis);
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ServiceTokenIssuer("too-short"));
    }

    @Test
    void blankServiceNameIsRejected() {
        var issuer = new ServiceTokenIssuer(SECRET);
        assertThrows(IllegalArgumentException.class, () -> issuer.issueFor(" "));
    }
}
