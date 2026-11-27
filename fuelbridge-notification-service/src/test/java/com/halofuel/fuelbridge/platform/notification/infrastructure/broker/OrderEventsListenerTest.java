package com.halofuel.fuelbridge.platform.notification.infrastructure.broker;

import com.halofuel.fuelbridge.platform.notification.application.commandservices.NotificationCommandService;
import com.halofuel.fuelbridge.platform.notification.application.outboundservices.CompanyUsers;
import com.halofuel.fuelbridge.platform.notification.domain.model.commands.CreateNotificationCommand;
import com.halofuel.fuelbridge.platform.notification.infrastructure.broker.OrderEventsListener.OrderIntegrationEventMessage;
import com.halofuel.fuelbridge.platform.notification.infrastructure.persistence.jpa.entities.ProcessedEventRecord;
import com.halofuel.fuelbridge.platform.notification.infrastructure.persistence.jpa.repositories.ProcessedEventRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderEventsListenerTest {

    private final ProcessedEventRecordRepository processedEvents = mock(ProcessedEventRecordRepository.class);
    private final CompanyUsers companyUsers = mock(CompanyUsers.class);
    private final NotificationCommandService notificationCommandService = mock(NotificationCommandService.class);
    private OrderEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderEventsListener(processedEvents, companyUsers, notificationCommandService);
    }

    @Test
    void aNewEventCreatesTheNotificationAndTheDedupeRecord() {
        var event = new OrderIntegrationEventMessage("evt-1", "FuelOrderConfirmed", 4101L, 101L, Instant.now());
        when(processedEvents.existsByEventId("evt-1")).thenReturn(false);
        when(companyUsers.findUserIdByCompanyId(101L)).thenReturn(Optional.of(1101L));

        listener.onOrderEvent(event);

        verify(notificationCommandService).handle(any(CreateNotificationCommand.class));
        verify(processedEvents).save(argThat((ProcessedEventRecord record) -> record.getEventId().equals("evt-1")));
    }

    @Test
    void aRepeatedEventIsDiscardedWithoutCreatingASecondNotification() {
        var event = new OrderIntegrationEventMessage("evt-1", "FuelOrderConfirmed", 4101L, 101L, Instant.now());
        when(processedEvents.existsByEventId("evt-1")).thenReturn(true);

        listener.onOrderEvent(event);

        verify(notificationCommandService, never()).handle(any(CreateNotificationCommand.class));
        verify(processedEvents, never()).save(any());
        verifyNoInteractions(companyUsers);
    }

    @Test
    void aCompanyWithNoIamUserIsDiscardedWithoutError() {
        var event = new OrderIntegrationEventMessage("evt-2", "FuelOrderCancelled", 4102L, 999L, Instant.now());
        when(processedEvents.existsByEventId("evt-2")).thenReturn(false);
        when(companyUsers.findUserIdByCompanyId(999L)).thenReturn(Optional.empty());

        listener.onOrderEvent(event);

        verify(notificationCommandService, never()).handle(any(CreateNotificationCommand.class));
        verify(processedEvents, never()).save(any());
    }
}
