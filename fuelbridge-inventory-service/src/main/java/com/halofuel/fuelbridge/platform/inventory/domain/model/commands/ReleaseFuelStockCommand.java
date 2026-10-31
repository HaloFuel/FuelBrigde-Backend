package com.halofuel.fuelbridge.platform.inventory.domain.model.commands;

/** Compensates a previous reservation when the delivery could not be persisted. */
public record ReleaseFuelStockCommand(Long fuelProductId, Double quantity) {
}
