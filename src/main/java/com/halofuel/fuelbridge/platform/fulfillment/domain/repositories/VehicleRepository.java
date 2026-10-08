package com.halofuel.fuelbridge.platform.fulfillment.domain.repositories;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    Optional<Vehicle> findById(Long id);
    List<Vehicle> findByProviderId(Long providerId);
    Vehicle save(Vehicle vehicle);
    void deleteById(Long id);
}
