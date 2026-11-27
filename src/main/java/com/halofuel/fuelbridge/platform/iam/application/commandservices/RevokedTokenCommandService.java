package com.halofuel.fuelbridge.platform.iam.application.commandservices;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.RevokedToken;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.RevokeTokenCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;

public interface RevokedTokenCommandService {
    Result<RevokedToken, ApplicationError> handle(RevokeTokenCommand command);
}
