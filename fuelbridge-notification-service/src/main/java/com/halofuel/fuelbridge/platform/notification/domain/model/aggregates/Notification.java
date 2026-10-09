package com.halofuel.fuelbridge.platform.notification.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.notification.domain.model.commands.CreateNotificationCommand;
import com.halofuel.fuelbridge.platform.notification.domain.model.valueobjects.NotificationType;
import com.halofuel.fuelbridge.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class Notification extends AbstractDomainAggregateRoot<Notification> {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private Long referenceId;
    private Date createdAt;

    public Notification(CreateNotificationCommand command) {
        this.userId = command.userId();
        this.type = command.type();
        this.title = command.title();
        this.message = command.message();
        this.referenceId = command.referenceId();
        this.read = false;
    }

    public void markAsRead() {
        this.read = true;
    }
}
