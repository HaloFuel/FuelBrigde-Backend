package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.repositories;

import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPersistenceRepository extends JpaRepository<UserPersistenceEntity, Long> {
    Optional<UserPersistenceEntity> findByUsername(String username);
    // First-match query with explicit ordering: a plain findByCompanyId would throw
    // IncorrectResultSizeDataAccessException if a company ever had more than one user.
    Optional<UserPersistenceEntity> findFirstByCompanyIdOrderByIdAsc(Long companyId);
    Optional<UserPersistenceEntity> findByProviderId(Long providerId);
    boolean existsByUsername(String username);
}
