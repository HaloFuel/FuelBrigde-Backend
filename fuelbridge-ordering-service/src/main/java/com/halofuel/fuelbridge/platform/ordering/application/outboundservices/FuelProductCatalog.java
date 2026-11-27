package com.halofuel.fuelbridge.platform.ordering.application.outboundservices;

import java.util.Optional;

/**
 * Outbound port through which Ordering reads fuel products owned by the Inventory
 * bounded context, now a standalone service. Ordering keeps only the data it copies
 * into requests and orders (name, fuel type, unit, price and provider).
 */
public interface FuelProductCatalog {

    /**
     * @return the product, or empty when Inventory reports that it does not exist.
     * @throws IllegalStateException when the Inventory service cannot be reached.
     */
    Optional<FuelProductSnapshot> findById(Long fuelProductId);

    record FuelProductSnapshot(Long id, String name, String fuelType, Double pricePerUnit, String unit,
                               Double availableStock, Double capacity, Long providerId, Boolean active) {
    }
}
