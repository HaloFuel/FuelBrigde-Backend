package com.halofuel.fuelbridge.platform.inventory.domain.model.commands;

import com.halofuel.fuelbridge.platform.inventory.domain.model.valueobjects.FuelType;

public record CreateFuelProductCommand(
        String name,
        FuelType fuelType,
        Double pricePerUnit,
        String unit,
        Double availableStock,
        Double capacity,
        Long providerId,
        Boolean active) {
}
