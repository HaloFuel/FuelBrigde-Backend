package com.halofuel.fuelbridge.platform.ordering.domain.model.commands;

/**
 * Sprint 1 (US-12): requests the dispatch of an existing fuel order.
 * The application service checks that the order exists before changing its state.
 *
 * @param orderId identifier of the fuel order to dispatch
 */
public record DispatchFuelOrderCommand(Long orderId) {
}
