package com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.iam.domain.model.commands.ResetPasswordCommand;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ResetPasswordResource;

public final class ResetPasswordCommandFromResourceAssembler {

    private ResetPasswordCommandFromResourceAssembler() {
    }

    public static ResetPasswordCommand toCommandFromResource(ResetPasswordResource resource) {
        return new ResetPasswordCommand(resource.token(), resource.newPassword());
    }
}
