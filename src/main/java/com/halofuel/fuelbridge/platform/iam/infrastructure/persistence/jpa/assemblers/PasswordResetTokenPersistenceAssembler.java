package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.assemblers;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.PasswordResetToken;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.entities.PasswordResetTokenPersistenceEntity;

public final class PasswordResetTokenPersistenceAssembler {

    private PasswordResetTokenPersistenceAssembler() {
    }

    public static PasswordResetToken toDomainFromPersistence(PasswordResetTokenPersistenceEntity entity) {
        if (entity == null) return null;
        var domain = new PasswordResetToken();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUserId());
        domain.setTokenHash(entity.getTokenHash());
        domain.setExpiresAt(entity.getExpiresAt());
        domain.setUsed(entity.isUsed());
        return domain;
    }

    public static PasswordResetTokenPersistenceEntity toPersistenceFromDomain(PasswordResetToken token) {
        if (token == null) return null;
        var entity = new PasswordResetTokenPersistenceEntity();
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setTokenHash(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setUsed(token.isUsed());
        return entity;
    }
}
