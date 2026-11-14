package com.halofuel.fuelbridge.platform.fulfillment.application.commandservices;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Delivery;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CompleteDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CreateDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.DispatchDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.FailDeliveryCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;

public interface DeliveryCommandService {
    Result<Delivery, ApplicationError> handle(CreateDeliveryCommand command);
    Result<Delivery, ApplicationError> handle(DispatchDeliveryCommand command);
    Result<Delivery, ApplicationError> handle(CompleteDeliveryCommand command);
    Result<Delivery, ApplicationError> handle(FailDeliveryCommand command);
}
