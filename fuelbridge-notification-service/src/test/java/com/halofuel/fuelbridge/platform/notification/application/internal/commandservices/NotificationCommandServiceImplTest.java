package com.halofuel.fuelbridge.platform.notification.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.notification.domain.model.aggregates.Notification;
import com.halofuel.fuelbridge.platform.notification.domain.model.commands.CreateNotificationCommand;
import com.halofuel.fuelbridge.platform.notification.domain.model.commands.MarkNotificationAsReadCommand;
import com.halofuel.fuelbridge.platform.notification.domain.model.valueobjects.NotificationType;
import com.halofuel.fuelbridge.platform.notification.domain.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests of the notification command service with a mocked repository. */
class NotificationCommandServiceImplTest {

    private NotificationRepository repository;
    private NotificationCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        service = new NotificationCommandServiceImpl(repository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createSavesAnUnreadNotification() {
        var result = service.handle(new CreateNotificationCommand(7L, NotificationType.ORDER_CONFIRMED,
                "Pedido confirmado", "El pedido número 15 ha sido confirmado.", 15L));

        assertTrue(result.isSuccess());
        assertFalse(result.toOptional().orElseThrow().isRead());
        verify(repository).save(any(Notification.class));
    }

    @Test
    void markAsReadPersistsTheReadFlag() {
        var notification = new Notification(new CreateNotificationCommand(7L, NotificationType.GENERAL,
                "Aviso", "Mensaje", null));
        when(repository.findById(3L)).thenReturn(Optional.of(notification));

        var result = service.handle(new MarkNotificationAsReadCommand(3L));

        assertTrue(result.isSuccess());
        assertTrue(notification.isRead());
        verify(repository).save(notification);
    }

    @Test
    void markAsReadFailsWithoutSavingWhenTheNotificationDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        var result = service.handle(new MarkNotificationAsReadCommand(99L));

        assertTrue(result.isFailure());
        verify(repository, never()).save(any());
    }
}
