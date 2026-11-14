package com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.UserProfileResource;

public final class UserProfileResourceFromEntityAssembler {

    private UserProfileResourceFromEntityAssembler() {
    }

    public static UserProfileResource toResourceFromEntity(User user) {
        return new UserProfileResource(user.getId(), user.getUsername(), user.getFullName(), user.getPhone());
    }
}
