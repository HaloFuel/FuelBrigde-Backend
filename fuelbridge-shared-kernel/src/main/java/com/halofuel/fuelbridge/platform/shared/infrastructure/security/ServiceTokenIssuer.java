package com.halofuel.fuelbridge.platform.shared.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.NullMarked;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * Issues the short-lived JWT that a FuelBridge application sends when it calls
 * another application (service-to-service). The token is signed with the same
 * shared secret that every service already uses to validate the tokens issued
 * by Identity &amp; Access, so the receiving service accepts it without
 * contacting the monolith.
 *
 * <p>Plain class on purpose: each application registers it as a bean with its
 * own {@code authorization.jwt.secret} value, for example
 * {@code new ServiceTokenIssuer(secret, Duration.ofMinutes(5))}.</p>
 */
@NullMarked
public final class ServiceTokenIssuer {

    private static final Duration DEFAULT_TIME_TO_LIVE = Duration.ofMinutes(5);
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration timeToLive;

    public ServiceTokenIssuer(String secret) {
        this(secret, DEFAULT_TIME_TO_LIVE);
    }

    public ServiceTokenIssuer(String secret, Duration timeToLive) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("The JWT secret must have at least 32 bytes");
        }
        if (timeToLive.isNegative() || timeToLive.isZero()) {
            throw new IllegalArgumentException("The token time to live must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.timeToLive = timeToLive;
    }

    /**
     * @param serviceName name of the calling application, stored as the token subject
     *                    (for example {@code fuelbridge-payment-service})
     * @return a compact signed JWT ready for an {@code Authorization: Bearer} header
     */
    public String issueFor(String serviceName) {
        if (serviceName.isBlank()) {
            throw new IllegalArgumentException("The service name must not be blank");
        }
        var issuedAt = new Date();
        var expiration = new Date(issuedAt.getTime() + timeToLive.toMillis());
        return Jwts.builder()
                .subject(serviceName)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }
}
