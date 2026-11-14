package com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Delivery;
import com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource;

public final class DeliveryResourceFromEntityAssembler {

    private DeliveryResourceFromEntityAssembler() {
    }

    public static DeliveryResource toResourceFromEntity(Delivery delivery) {
        return new DeliveryResource(delivery.getId(), delivery.getOrderId(), delivery.getProviderId(),
                delivery.getDriverId(), delivery.getVehicleId(), delivery.getStatus(),
                delivery.getScheduledDate(), delivery.getDispatchedAt(),
                delivery.getDeliveredAt(), delivery.getNotes());
    }
}
