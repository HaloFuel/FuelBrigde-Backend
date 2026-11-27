package com.halofuel.fuelbridge.platform.ordering.domain.model.events;

/**
 * Sprint 1 - US-29: Fuel order cancellation event.
 *
 * This event is generated when a fuel order is cancelled.
 * It allows the Notification module to notify the buyer
 * about the cancellation of the order.
 *
 * @param orderId identifier of the cancelled fuel order
 * @param companyId identifier of the buyer company
 */
public record FuelOrderCancelledEvent(Long orderId, Long companyId) {
}
