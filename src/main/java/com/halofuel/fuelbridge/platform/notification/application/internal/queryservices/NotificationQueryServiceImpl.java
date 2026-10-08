package com.halofuel.fuelbridge.platform.notification.application.internal.queryservices;

import com.halofuel.fuelbridge.platform.notification.application.queryservices.NotificationQueryService;
import com.halofuel.fuelbridge.platform.notification.domain.model.aggregates.Notification;
import com.halofuel.fuelbridge.platform.notification.domain.model.queries.GetNotificationByIdQuery;
import com.halofuel.fuelbridge.platform.notification.domain.model.queries.GetNotificationsByUserIdQuery;
import com.halofuel.fuelbridge.platform.notification.domain.model.queries.GetUnreadNotificationsByUserIdQuery;
import com.halofuel.fuelbridge.platform.notification.domain.repositories.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Optional<Notification> handle(GetNotificationByIdQuery query) {
        return notificationRepository.findById(query.notificationId());
    }

    @Override
    public List<Notification> handle(GetNotificationsByUserIdQuery query) {
        return notificationRepository.findByUserId(query.userId());
    }

    @Override
    public List<Notification> handle(GetUnreadNotificationsByUserIdQuery query) {
        return notificationRepository.findByUserIdAndReadFalse(query.userId());
    }
}