package com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.iam.domain.model.commands.UpdateUserProfileCommand;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.UpdateUserProfileResource;

public final class UpdateUserProfileCommandFromResourceAssembler {

    private UpdateUserProfileCommandFromResourceAssembler() {
    }

    public static UpdateUserProfileCommand toCommandFromResource(Long userId, UpdateUserProfileResource resource) {
        return new UpdateUserProfileCommand(userId, resource.fullName(), resource.phone());
    }
}
