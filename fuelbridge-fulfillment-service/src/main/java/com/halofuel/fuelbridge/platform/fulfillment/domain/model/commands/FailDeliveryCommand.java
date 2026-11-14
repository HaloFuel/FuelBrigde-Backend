package com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands;

public record FailDeliveryCommand(Long deliveryId, String reason) {
}
