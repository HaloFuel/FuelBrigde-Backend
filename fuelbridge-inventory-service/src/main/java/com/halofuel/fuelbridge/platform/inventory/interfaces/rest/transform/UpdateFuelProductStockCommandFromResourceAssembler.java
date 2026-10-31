package com.halofuel.fuelbridge.platform.inventory.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.UpdateFuelProductStockCommand;
import com.halofuel.fuelbridge.platform.inventory.interfaces.rest.resources.UpdateFuelProductStockResource;

public final class UpdateFuelProductStockCommandFromResourceAssembler {

    private UpdateFuelProductStockCommandFromResourceAssembler() {
    }

    public static UpdateFuelProductStockCommand toCommandFromResource(Long fuelProductId,
                                                                      UpdateFuelProductStockResource resource) {
        return new UpdateFuelProductStockCommand(fuelProductId, resource.newStock());
    }
}