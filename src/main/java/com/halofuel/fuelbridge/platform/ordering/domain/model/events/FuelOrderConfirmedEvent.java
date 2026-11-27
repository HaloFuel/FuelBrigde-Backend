package com.halofuel.fuelbridge.platform.ordering.domain.model.events;

/**
 * Sprint 1 - US-29: Fuel order confirmation event.
 *
 * This event is generated when a fuel order is confirmed.
 * It allows the Notification module to inform the buyer
 * about the confirmation of the order.
 *
 * @param orderId identifier of the confirmed fuel order
 * @param companyId identifier of the buyer company
 */
public record FuelOrderConfirmedEvent(Long orderId, Long companyId) {
}
