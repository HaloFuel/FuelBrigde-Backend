package com.halofuel.fuelbridge.platform.fulfillment.domain.repositories;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Driver;

import java.util.List;
import java.util.Optional;

public interface DriverRepository {
    Optional<Driver> findById(Long id);
    List<Driver> findByProviderId(Long providerId);
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);
    Driver save(Driver driver);
    void deleteById(Long id);
}
