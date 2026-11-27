package com.halofuel.fuelbridge.platform.notification.application.commandservices;

import com.halofuel.fuelbridge.platform.notification.domain.model.aggregates.Notification;
import com.halofuel.fuelbridge.platform.notification.domain.model.commands.CreateNotificationCommand;
import com.halofuel.fuelbridge.platform.notification.domain.model.commands.MarkNotificationAsReadCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;

public interface NotificationCommandService {
    Result<Notification, ApplicationError> handle(CreateNotificationCommand command);
    Result<Notification, ApplicationError> handle(MarkNotificationAsReadCommand command);
}
