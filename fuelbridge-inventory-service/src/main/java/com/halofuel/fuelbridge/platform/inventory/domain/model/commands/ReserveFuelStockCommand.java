package com.halofuel.fuelbridge.platform.inventory.domain.model.commands;

/** Withdraws stock from a fuel product when Fulfillment schedules a delivery. */
public record ReserveFuelStockCommand(Long fuelProductId, Double quantity) {
}
