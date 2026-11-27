package com.halofuel.fuelbridge.platform.equipment.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.equipment.domain.model.commands.ReceiveFuelCommand;
import com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources.ReceiveFuelResource;

public final class ReceiveFuelCommandFromResourceAssembler {

    private ReceiveFuelCommandFromResourceAssembler() {
    }

    public static ReceiveFuelCommand toCommandFromResource(Long equipmentId, ReceiveFuelResource resource) {
        return new ReceiveFuelCommand(equipmentId, resource == null ? null : resource.quantity());
    }
}
