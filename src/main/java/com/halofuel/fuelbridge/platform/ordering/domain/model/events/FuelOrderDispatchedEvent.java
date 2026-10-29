package com.halofuel.fuelbridge.platform.ordering.domain.model.events;

public record FuelOrderDispatchedEvent(Long orderId, Long companyId) {
}
