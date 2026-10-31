package com.halofuel.fuelbridge.platform.inventory.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.CreateFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.CreateFuelProductResource;

public final class CreateFuelProductCommandFromResourceAssembler {

    private CreateFuelProductCommandFromResourceAssembler() {
    }

    public static CreateFuelProductCommand toCommandFromResource(CreateFuelProductResource resource) {
        return new CreateFuelProductCommand(resource.name(), resource.fuelType(), resource.pricePerUnit(),
                resource.unit(), resource.availableStock(), resource.capacity(), resource.providerId(),
                resource.active());
    }
}
