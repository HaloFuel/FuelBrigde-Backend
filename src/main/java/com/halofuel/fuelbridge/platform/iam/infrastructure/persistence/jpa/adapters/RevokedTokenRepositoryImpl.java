package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.adapters;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.RevokedToken;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.RevokedTokenRepository;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.assemblers.RevokedTokenPersistenceAssembler;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.repositories.RevokedTokenPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class RevokedTokenRepositoryImpl implements RevokedTokenRepository {

    private final RevokedTokenPersistenceRepository repository;

    public RevokedTokenRepositoryImpl(RevokedTokenPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public RevokedToken save(RevokedToken revokedToken) {
        var entity = RevokedTokenPersistenceAssembler.toPersistenceFromDomain(revokedToken);
        return RevokedTokenPersistenceAssembler.toDomainFromPersistence(repository.save(entity));
    }

    @Override
    public boolean existsByTokenHash(String tokenHash) {
        return repository.existsByTokenHash(tokenHash);
    }

    @Override
    public void deleteByExpirationDateBefore(Instant instant) {
        repository.deleteExpiredBefore(instant);
    }
}
