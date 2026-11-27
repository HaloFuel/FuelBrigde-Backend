package com.halofuel.fuelbridge.platform.notification.application.queryservices;

import com.halofuel.fuelbridge.platform.notification.domain.model.aggregates.Notification;
import com.halofuel.fuelbridge.platform.notification.domain.model.queries.GetNotificationByIdQuery;
import com.halofuel.fuelbridge.platform.notification.domain.model.queries.GetNotificationsByUserIdQuery;
import com.halofuel.fuelbridge.platform.notification.domain.model.queries.GetUnreadNotificationsByUserIdQuery;

import java.util.List;
import java.util.Optional;

public interface NotificationQueryService {
    Optional<Notification> handle(GetNotificationByIdQuery query);
    List<Notification> handle(GetNotificationsByUserIdQuery query);
    List<Notification> handle(GetUnreadNotificationsByUserIdQuery query);
}
