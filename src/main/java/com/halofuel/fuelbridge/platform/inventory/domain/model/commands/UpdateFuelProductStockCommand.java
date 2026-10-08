package com.halofuel.fuelbridge.platform.inventory.domain.model.commands;

public record UpdateFuelProductStockCommand(Long fuelProductId, Double newStock) {
}

