package com.halofuel.fuelbridge.platform.equipment.application.commandservices;

import com.halofuel.fuelbridge.platform.equipment.domain.model.aggregates.Equipment;
import com.halofuel.fuelbridge.platform.equipment.domain.model.commands.CreateEquipmentCommand;
import com.halofuel.fuelbridge.platform.equipment.domain.model.commands.UpdateEquipmentCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;

public interface EquipmentCommandService {
    Result<Equipment, ApplicationError> handle(CreateEquipmentCommand command);

    Result<Equipment, ApplicationError> handle(UpdateEquipmentCommand command);
}
//
