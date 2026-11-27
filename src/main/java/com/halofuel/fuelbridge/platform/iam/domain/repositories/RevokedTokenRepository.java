package com.halofuel.fuelbridge.platform.iam.domain.repositories;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.RevokedToken;

import java.time.Instant;

public interface RevokedTokenRepository {
    RevokedToken save(RevokedToken revokedToken);
    boolean existsByTokenHash(String tokenHash);
    void deleteByExpirationDateBefore(Instant instant);
}
