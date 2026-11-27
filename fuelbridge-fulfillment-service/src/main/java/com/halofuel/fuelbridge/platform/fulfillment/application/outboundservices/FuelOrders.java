package com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices;

import java.util.Optional;

/**
 * Outbound port through which Fulfillment reads fuel orders and requests their
 * state transitions from the Ordering bounded context, which remains in the
 * FuelBridge Platform. Replaces the direct use of the Ordering repository that
 * existed while Fulfillment lived inside the monolith.
 */
public interface FuelOrders {

    /**
     * @return the order, or empty when Ordering does not know it
     * @throws OrderingUnavailableException when Ordering cannot be reached or answers with an unexpected error
     */
    Optional<FuelOrderSnapshot> findById(Long orderId);

    /** Requests POST /api/v1/fuel-orders/{orderId}/dispatch (PENDING to DISPATCHED). */
    TransitionOutcome dispatch(Long orderId);

    /** Requests POST /api/v1/fuel-orders/{orderId}/receive (DISPATCHED to PENDING_PAYMENT). */
    TransitionOutcome receive(Long orderId);

    /**
     * Saga compensation: requests POST /api/v1/fuel-orders/{orderId}/revert-dispatch
     * (DISPATCHED to PENDING) when the local transaction fails after the dispatch was applied.
     */
    TransitionOutcome revertDispatch(Long orderId);

    /** Fields of the Ordering FuelOrderResource that Fulfillment needs. */
    record FuelOrderSnapshot(Long id, Long providerId, Long fuelProductId, Long equipmentId,
                             Double requestedQuantity, String status) {

        public boolean isPending() {
            return "PENDING".equals(status);
        }

        public boolean isDispatched() {
            return "DISPATCHED".equals(status);
        }
    }

    enum TransitionOutcome {
        APPLIED,
        REJECTED,
        ORDER_NOT_FOUND,
        UNAVAILABLE
    }

    class OrderingUnavailableException extends RuntimeException {
        public OrderingUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
