package com.halofuel.fuelbridge.platform.fulfillment.application.outboundservices;

/**
 * Outbound port through which Fulfillment informs the Equipment bounded context,
 * now a standalone service, of the fuel a completed delivery left in a tank.
 */
public interface EquipmentFuelReceipts {

    FuelReceiptOutcome register(Long equipmentId, Double quantity);

    enum FuelReceiptOutcome {
        REGISTERED,
        EQUIPMENT_NOT_FOUND,
        UNAVAILABLE
    }
}
