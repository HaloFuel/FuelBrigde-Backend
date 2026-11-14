package com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CreateDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.CreateDeliveryResource;

public final class CreateDeliveryCommandFromResourceAssembler {

    private CreateDeliveryCommandFromResourceAssembler() {
    }

    public static CreateDeliveryCommand toCommandFromResource(CreateDeliveryResource resource) {
        return new CreateDeliveryCommand(resource.orderId(), resource.providerId(), resource.driverId(),
                resource.vehicleId(), resource.scheduledDate(), resource.notes());
    }
}
