package com.halofuel.fuelbridge.platform.ordering.domain.model.commands;

import java.time.LocalDate;

/**
 * Data needed to register a fuel order. The equipment and the scheduled date are
 * optional; every other field is required (TS-04: an incomplete request answers 400).
 */
public record CreateFuelOrderCommand(
        Long companyId,
        Long providerId,
        Long fuelProductId,
        Long equipmentId,
        Double requestedQuantity,
        String deliveryAddress,
        LocalDate scheduledDate) {

    public CreateFuelOrderCommand {
        if (companyId == null) throw new IllegalArgumentException("companyId is required");
        if (providerId == null) throw new IllegalArgumentException("providerId is required");
        if (fuelProductId == null) throw new IllegalArgumentException("fuelProductId is required");
        if (requestedQuantity == null || requestedQuantity <= 0) {
            throw new IllegalArgumentException("requestedQuantity must be greater than zero");
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new IllegalArgumentException("deliveryAddress is required");
        }
    }
}
