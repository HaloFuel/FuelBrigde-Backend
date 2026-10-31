package com.halofuel.fuelbridge.platform.notification.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.notification.domain.model.commands.CreateNotificationCommand;
import com.halofuel.fuelbridge.platform.notification.domain.model.valueobjects.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests of the Notification aggregate. */
class NotificationTest {

    private static Notification dispatchedNotification() {
        return new Notification(new CreateNotificationCommand(7L, NotificationType.ORDER_DISPATCHED,
                "Pedido despachado", "El pedido número 15 ha sido despachado.", 15L));
    }

    @Test
    void aNewNotificationCopiesTheCommandAndStartsUnread() {
        var notification = dispatchedNotification();
        assertEquals(7L, notification.getUserId());
        assertEquals(NotificationType.ORDER_DISPATCHED, notification.getType());
        assertEquals("Pedido despachado", notification.getTitle());
        assertEquals(15L, notification.getReferenceId());
        assertFalse(notification.isRead());
    }

    @Test
    void markAsReadIsIdempotent() {
        var notification = dispatchedNotification();
        notification.markAsRead();
        notification.markAsRead();
        assertTrue(notification.isRead());
    }
}
