package com.halofuel.fuelbridge.platform.notification.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.notification.domain.model.commands.CreateNotificationCommand;
import com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.CreateNotificationResource;

public final class CreateNotificationCommandFromResourceAssembler {

    private CreateNotificationCommandFromResourceAssembler() {
    }

    public static CreateNotificationCommand toCommandFromResource(CreateNotificationResource resource) {
        return new CreateNotificationCommand(resource.userId(), resource.type(),
                resource.title(), resource.message(), resource.referenceId());
    }
}
