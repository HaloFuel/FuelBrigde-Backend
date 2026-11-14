package com.halofuel.fuelbridge.platform.iam.domain.model.valueobjects;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 hashing of opaque tokens so that only the hash is ever persisted. */
public final class TokenHash {

    private TokenHash() {
    }

    /** Lower-case hex SHA-256 of the token (64 characters). */
    public static String sha256(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
