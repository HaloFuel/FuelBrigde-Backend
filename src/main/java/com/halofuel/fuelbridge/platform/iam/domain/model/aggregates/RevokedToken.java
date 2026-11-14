package com.halofuel.fuelbridge.platform.iam.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * A JWT that was revoked before its natural expiration (sign-out). Only the SHA-256 hash
 * of the token is kept, never the token itself, and the entry is needed only until the
 * token would have expired anyway.
 */
@Getter
public class RevokedToken extends AbstractDomainAggregateRoot<RevokedToken> {

    @Setter private Long id;
    @Setter private String tokenHash;
    @Setter private Instant expirationDate;
    @Setter private Instant revokedAt;

    public RevokedToken() {
    }

    public RevokedToken(String token, Instant expirationDate, Instant revokedAt) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        if (expirationDate == null) {
            throw new IllegalArgumentException("Expiration date is required");
        }
        this.tokenHash = hash(token);
        this.expirationDate = expirationDate;
        this.revokedAt = revokedAt == null ? Instant.now() : revokedAt;
    }

    /** Lower-case hex SHA-256 of the token (64 characters). */
    public static String hash(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
