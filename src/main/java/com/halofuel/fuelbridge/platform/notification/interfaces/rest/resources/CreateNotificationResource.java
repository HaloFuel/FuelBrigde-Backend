package com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources;

import com.halofuel.fuelbridge.platform.notification.domain.model.valueobjects.NotificationType;

public record CreateNotificationResource(Long userId, Long companyId, Long providerId,
                                         NotificationType type, String title, String message,
                                         Long referenceId) {
}