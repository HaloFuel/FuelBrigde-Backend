package com.halofuel.fuelbridge.platform.iam.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.iam.domain.model.valueobjects.TokenHash;
import com.halofuel.fuelbridge.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

/**
 * A single-use, time-limited token that lets a user choose a new password. Only the SHA-256
 * hash of the token is kept; the raw token exists only in the response of the request that
 * issued it.
 */
@Getter
public class PasswordResetToken extends AbstractDomainAggregateRoot<PasswordResetToken> {

    public static final Duration VALIDITY = Duration.ofHours(1);

    @Setter private Long id;
    @Setter private Long userId;
    @Setter private String tokenHash;
    @Setter private Instant expiresAt;
    @Setter private boolean used;

    public PasswordResetToken() {
    }

    public PasswordResetToken(Long userId, String rawToken, Instant expiresAt) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration is required");
        }
        this.userId = userId;
        this.tokenHash = hash(rawToken);
        this.expiresAt = expiresAt;
        this.used = false;
    }

    /** Creates a token that is valid for {@link #VALIDITY} from {@code now}. */
    public static PasswordResetToken issue(Long userId, String rawToken, Instant now) {
        return new PasswordResetToken(userId, rawToken, now.plus(VALIDITY));
    }

    public static String hash(String rawToken) {
        return TokenHash.sha256(rawToken);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsable(Instant now) {
        return !used && !isExpired(now);
    }

    /** Consumes the token. Fails when it was already used or has expired. */
    public void markUsed(Instant now) {
        if (used) {
            throw new IllegalStateException("Password reset token was already used");
        }
        if (isExpired(now)) {
            throw new IllegalStateException("Password reset token has expired");
        }
        this.used = true;
    }
}
