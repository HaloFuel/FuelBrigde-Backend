package com.halofuel.fuelbridge.platform.ordering.domain.model.commands;

/**
 * Sprint 4 (T10.05): compensation of the delivery Saga. Returns a DISPATCHED order to
 * PENDING when Fulfillment cannot commit the delivery after the dispatch was applied.
 *
 * @param orderId identifier of the fuel order whose dispatch is reverted
 */
public record RevertFuelOrderDispatchCommand(Long orderId) {
}
