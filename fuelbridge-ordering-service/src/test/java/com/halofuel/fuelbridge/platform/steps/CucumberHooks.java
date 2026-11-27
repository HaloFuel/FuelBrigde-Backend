package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.FuelOrderPersistenceRepository;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.repositories.FuelRequestPersistenceRepository;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;

/** Keeps scenarios independent by clearing orders, requests and the remote stand-ins. */
public class CucumberHooks {

    @Autowired
    private FuelOrderPersistenceRepository orders;

    @Autowired
    private FuelRequestPersistenceRepository requests;

    @Autowired
    private InventoryHttpStub inventoryService;

    @Autowired
    private NotificationHttpStub notificationService;

    @Autowired
    private IamHttpStub iamService;

    @Autowired
    private CucumberSpringConfiguration.DispatchEventRecorder dispatchEvents;

    @Before
    public void clearOrderingData() {
        orders.deleteAll();
        requests.deleteAll();
        inventoryService.reset();
        notificationService.clear();
        iamService.reset();
        dispatchEvents.clear();
    }
}
