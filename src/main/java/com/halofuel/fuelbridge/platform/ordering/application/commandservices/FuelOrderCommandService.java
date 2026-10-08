package com.halofuel.fuelbridge.platform.ordering.application.commandservices;

import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CancelFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.ConfirmFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CreateFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.DispatchFuelOrderCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;

public interface FuelOrderCommandService {
    Result<FuelOrder, ApplicationError> handle(CreateFuelOrderCommand command);
    Result<FuelOrder, ApplicationError> handle(ConfirmFuelOrderCommand command);
    Result<FuelOrder, ApplicationError> handle(CancelFuelOrderCommand command);
    Result<FuelOrder, ApplicationError> handle(DispatchFuelOrderCommand command);
}
