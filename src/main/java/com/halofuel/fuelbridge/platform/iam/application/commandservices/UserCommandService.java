package com.halofuel.fuelbridge.platform.iam.application.commandservices;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.SignInCommand;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.SignUpCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.apache.commons.lang3.tuple.ImmutablePair;

public interface UserCommandService {
    Result<ImmutablePair<User, String>, ApplicationError> handle(SignInCommand command);
    Result<User, ApplicationError> handle(SignUpCommand command);
}
