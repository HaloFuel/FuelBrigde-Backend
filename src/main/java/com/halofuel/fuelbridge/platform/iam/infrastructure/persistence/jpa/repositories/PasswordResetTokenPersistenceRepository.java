package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.repositories;

import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.entities.PasswordResetTokenPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenPersistenceRepository
        extends JpaRepository<PasswordResetTokenPersistenceEntity, Long> {

    Optional<PasswordResetTokenPersistenceEntity> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("update PasswordResetTokenPersistenceEntity t set t.used = true where t.userId = :userId and t.used = false")
    void invalidateUnusedByUserId(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("delete from PasswordResetTokenPersistenceEntity t where t.expiresAt < :instant")
    void deleteExpiredBefore(@Param("instant") Instant instant);
}
