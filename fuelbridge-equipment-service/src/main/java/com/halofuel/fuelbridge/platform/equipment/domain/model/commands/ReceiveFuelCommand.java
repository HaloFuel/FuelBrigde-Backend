package com.halofuel.fuelbridge.platform.equipment.domain.model.commands;

/** Registers fuel delivered to a piece of equipment once Fulfillment completes the delivery. */
public record ReceiveFuelCommand(Long equipmentId, Double quantity) {
}
