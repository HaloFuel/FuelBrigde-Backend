package com.halofuel.fuelbridge.platform.ordering.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource;

public final class FuelOrderResourceFromEntityAssembler {

    private FuelOrderResourceFromEntityAssembler() {
    }

    public static FuelOrderResource toResourceFromEntity(FuelOrder order) {
        return new FuelOrderResource(order.getId(), order.getRequestId(), order.getCompanyId(), order.getProviderId(),
                order.getFuelProductId(), order.getEquipmentId(), order.getRequestedQuantity(),
                order.getTotalPrice(), order.getStatus(), order.getDeliveryAddress(),
                order.getScheduledDate());
    }
}
