package com.halofuel.fuelbridge.platform.iam.domain.repositories;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    /**
     * Returns the user of a buyer company. Nothing in the model forbids several users per company, so
     * the one with the lowest id (the first registered) is returned to keep the result deterministic.
     */
    Optional<User> findByCompanyId(Long companyId);
    Optional<User> findByProviderId(Long providerId);
    List<User> findAll();
    User save(User user);
    boolean existsByUsername(String username);
}
