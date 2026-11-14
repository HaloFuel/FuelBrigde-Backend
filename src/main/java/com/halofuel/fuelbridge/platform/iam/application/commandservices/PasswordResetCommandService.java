package com.halofuel.fuelbridge.platform.iam.application.commandservices;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.PasswordResetToken;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.ResetPasswordCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.apache.commons.lang3.tuple.ImmutablePair;

public interface PasswordResetCommandService {
    /** Returns the stored token (hash only) paired with the raw token to hand to the user. */
    Result<ImmutablePair<PasswordResetToken, String>, ApplicationError> handle(RequestPasswordResetCommand command);
    Result<Void, ApplicationError> handle(ResetPasswordCommand command);
}
