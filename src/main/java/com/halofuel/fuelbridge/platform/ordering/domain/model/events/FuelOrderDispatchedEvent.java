package com.halofuel.fuelbridge.platform.ordering.domain.model.events;

/**
 * Sprint 1 - US-30: Fuel order dispatch event.
 *
 * This event is generated when a fuel order is dispatched.
 * It allows the Notification module to inform the buyer
 * that the fuel order has been dispatched.
 *
 * @param orderId identifier of the dispatched fuel order
 * @param companyId identifier of the buyer company
 */
public record FuelOrderDispatchedEvent(Long orderId, Long companyId) {
}
