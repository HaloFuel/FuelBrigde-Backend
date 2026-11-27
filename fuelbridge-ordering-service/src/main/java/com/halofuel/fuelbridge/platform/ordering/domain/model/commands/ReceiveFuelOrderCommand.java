package com.halofuel.fuelbridge.platform.ordering.domain.model.commands;

/**
 * Sprint 3 (T09.01): registers that a dispatched fuel order was received.
 * Fulfillment requests this transition through REST when a delivery is
 * completed, so it no longer writes to the Ordering repository directly.
 *
 * @param orderId identifier of the fuel order that was received
 */
public record ReceiveFuelOrderCommand(Long orderId) {
}
