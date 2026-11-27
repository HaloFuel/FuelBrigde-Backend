package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.assemblers;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.RevokedToken;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.entities.RevokedTokenPersistenceEntity;

public final class RevokedTokenPersistenceAssembler {

    private RevokedTokenPersistenceAssembler() {
    }

    public static RevokedToken toDomainFromPersistence(RevokedTokenPersistenceEntity entity) {
        if (entity == null) return null;
        var domain = new RevokedToken();
        domain.setId(entity.getId());
        domain.setTokenHash(entity.getTokenHash());
        domain.setExpirationDate(entity.getExpirationDate());
        domain.setRevokedAt(entity.getRevokedAt());
        return domain;
    }

    public static RevokedTokenPersistenceEntity toPersistenceFromDomain(RevokedToken revokedToken) {
        if (revokedToken == null) return null;
        var entity = new RevokedTokenPersistenceEntity();
        entity.setId(revokedToken.getId());
        entity.setTokenHash(revokedToken.getTokenHash());
        entity.setExpirationDate(revokedToken.getExpirationDate());
        entity.setRevokedAt(revokedToken.getRevokedAt());
        return entity;
    }
}
