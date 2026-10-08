package com.halofuel.fuelbridge.platform.inventory.application.commandservices;

import com.halofuel.fuelbridge.platform.inventory.domain.model.aggregates.FuelProduct;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.CreateFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.DeleteFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.UpdateFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.UpdateFuelProductStockCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;

public interface FuelProductCommandService {
    Result<FuelProduct, ApplicationError> handle(CreateFuelProductCommand command);
    Result<FuelProduct, ApplicationError> handle(UpdateFuelProductStockCommand command);
    Result<FuelProduct, ApplicationError> handle(UpdateFuelProductCommand command);
    Result<Long, ApplicationError> handle(DeleteFuelProductCommand command);
}
