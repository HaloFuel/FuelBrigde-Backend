package com.halofuel.fuelbridge.platform.ordering.domain.model.events;

public record FuelOrderCancelledEvent(Long orderId, Long companyId) {
}
