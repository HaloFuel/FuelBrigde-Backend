package com.halofuel.fuelbridge.platform.notification.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.notification.domain.model.aggregates.Notification;
import com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.NotificationResource;

public final class NotificationResourceFromEntityAssembler {

    private NotificationResourceFromEntityAssembler() {
    }

    public static NotificationResource toResourceFromEntity(Notification notification) {
        return new NotificationResource(notification.getId(), notification.getUserId(),
                notification.getType(), notification.getTitle(), notification.getMessage(),
                notification.isRead(), notification.getReferenceId(),
                notification.getCreatedAt() != null ? notification.getCreatedAt().toInstant().toString() : null);
    }
}
