package com.halofuel.fuelbridge.platform.ordering.domain.model.events;

public record FuelOrderConfirmedEvent(Long orderId, Long companyId) {
}
