package com.halofuel.fuelbridge.platform.notification.application.internal.eventhandlers;

import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;
import com.halofuel.fuelbridge.platform.notification.application.commandservices.NotificationCommandService;
import com.halofuel.fuelbridge.platform.notification.domain.model.commands.CreateNotificationCommand;
import com.halofuel.fuelbridge.platform.notification.domain.model.valueobjects.NotificationType;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderCancelledEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderConfirmedEvent;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderDispatchedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class FuelOrderNotificationEventHandler {

    private final UserRepository userRepository;
    private final NotificationCommandService notificationCommandService;

    public FuelOrderNotificationEventHandler(UserRepository userRepository,
                                             NotificationCommandService notificationCommandService) {
        this.userRepository = userRepository;
        this.notificationCommandService = notificationCommandService;
    }

    @EventListener
    public void on(FuelOrderConfirmedEvent event) {
        userRepository.findByCompanyId(event.companyId()).ifPresent(user ->
                notificationCommandService.handle(new CreateNotificationCommand(
                        user.getId(),
                        NotificationType.ORDER_CONFIRMED,
                        "Pedido confirmado",
                        "El pedido número " + event.orderId() + " ha sido confirmado.",
                        event.orderId())));
    }

    @EventListener
    public void on(FuelOrderCancelledEvent event) {
        userRepository.findByCompanyId(event.companyId()).ifPresent(user ->
                notificationCommandService.handle(new CreateNotificationCommand(
                        user.getId(),
                        NotificationType.ORDER_CANCELLED,
                        "Pedido cancelado",
                        "El pedido número " + event.orderId() + " ha sido cancelado.",
                        event.orderId())));
    }

    @EventListener
    public void on(FuelOrderDispatchedEvent event) {
        userRepository.findByCompanyId(event.companyId()).ifPresent(user ->
                notificationCommandService.handle(new CreateNotificationCommand(
                        user.getId(),
                        NotificationType.ORDER_DISPATCHED,
                        "Pedido despachado",
                        "El pedido número " + event.orderId() + " ha sido despachado.",
                        event.orderId())));
    }
}
