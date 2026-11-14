package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.repositories;

import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.entities.RevokedTokenPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface RevokedTokenPersistenceRepository extends JpaRepository<RevokedTokenPersistenceEntity, Long> {
    boolean existsByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("delete from RevokedTokenPersistenceEntity t where t.expirationDate < :instant")
    void deleteExpiredBefore(@Param("instant") Instant instant);
}
