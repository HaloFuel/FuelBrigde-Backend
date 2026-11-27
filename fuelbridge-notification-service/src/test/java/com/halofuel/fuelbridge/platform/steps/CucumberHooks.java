package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.notification.infrastructure.persistence.jpa.repositories.NotificationPersistenceRepository;

import io.cucumber.java.Before;

import org.springframework.beans.factory.annotation.Autowired;

/** Keeps scenarios independent by clearing persisted notifications before each one runs. */
public class CucumberHooks {

    @Autowired
    private NotificationPersistenceRepository notifications;

    @Before
    public void clearNotifications() {
        notifications.deleteAll();
    }
}
