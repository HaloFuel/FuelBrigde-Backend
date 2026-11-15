package com.halofuel.fuelbridge.platform.payment.application.outboundservices;

/**
 * Outbound port through which Payment requests the mark-paid transition from the
 * Ordering bounded context, which remains in the FuelBridge Platform. Replaces the
 * direct use of the Ordering repository that existed while Payment lived inside
 * the monolith.
 */
public interface FuelOrders {

    /** Requests POST /api/v1/fuel-orders/{orderId}/mark-paid. */
    TransitionOutcome markPaid(Long orderId);

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
