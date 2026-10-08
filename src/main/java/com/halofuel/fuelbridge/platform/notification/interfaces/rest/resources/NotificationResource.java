package com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources;

import com.halofuel.fuelbridge.platform.notification.domain.model.valueobjects.NotificationType;

public record NotificationResource(Long id, Long userId, NotificationType type,
                                   String title, String message, boolean read, Long referenceId,
                                   String createdAt) {
}
