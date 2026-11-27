package com.halofuel.fuelbridge.platform.ordering.domain.model.commands;

/**
 * Sprint 3 (T09.01): marks an existing fuel order as paid. Payment requests this
 * transition through REST once a payment is completed, so it no longer writes
 * to the Ordering repository directly.
 *
 * @param orderId identifier of the fuel order that was paid
 */
public record MarkFuelOrderPaidCommand(Long orderId) {
}
