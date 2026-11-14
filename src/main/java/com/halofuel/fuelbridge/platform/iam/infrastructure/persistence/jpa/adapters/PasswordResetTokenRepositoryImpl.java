package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.adapters;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.PasswordResetToken;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.PasswordResetTokenRepository;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.assemblers.PasswordResetTokenPersistenceAssembler;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.repositories.PasswordResetTokenPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenPersistenceRepository repository;

    public PasswordResetTokenRepositoryImpl(PasswordResetTokenPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        var entity = PasswordResetTokenPersistenceAssembler.toPersistenceFromDomain(token);
        return PasswordResetTokenPersistenceAssembler.toDomainFromPersistence(repository.save(entity));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash)
                .map(PasswordResetTokenPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public void invalidateUnusedByUserId(Long userId) {
        repository.invalidateUnusedByUserId(userId);
    }

    @Override
    public void deleteByExpiresAtBefore(Instant instant) {
        repository.deleteExpiredBefore(instant);
    }
}
