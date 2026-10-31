package com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices;

/**
 * Outbound port through which Fulfillment reserves and, as compensation, releases
 * fuel stock owned by the Inventory bounded context, now a standalone service.
 */
public interface InventoryStock {

    StockReservationOutcome reserve(Long fuelProductId, Double quantity);

    /** Best-effort compensation of a previous reservation; failures are logged, not thrown. */
    void release(Long fuelProductId, Double quantity);

    enum StockReservationOutcome {
        RESERVED,
        INSUFFICIENT_STOCK,
        PRODUCT_NOT_FOUND,
        UNAVAILABLE
    }
}
