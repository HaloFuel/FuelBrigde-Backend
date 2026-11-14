package com.halofuel.fuelbridge.platform.iam.domain.repositories;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    /** Marks every still-unused token of the user as used (a new request supersedes older ones). */
    void invalidateUnusedByUserId(Long userId);
    void deleteByExpiresAtBefore(Instant instant);
}
