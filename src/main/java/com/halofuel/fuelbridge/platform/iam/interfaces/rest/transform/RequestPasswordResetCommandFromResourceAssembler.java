package com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ForgotPasswordResource;

public final class RequestPasswordResetCommandFromResourceAssembler {

    private RequestPasswordResetCommandFromResourceAssembler() {
    }

    /** Usernames are the account's email address, so either field identifies the user. */
    public static RequestPasswordResetCommand toCommandFromResource(ForgotPasswordResource resource) {
        var identifier = resource.username() != null && !resource.username().isBlank()
                ? resource.username()
                : resource.email();
        return new RequestPasswordResetCommand(identifier);
    }
}
