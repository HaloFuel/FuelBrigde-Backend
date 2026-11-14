package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.fulfillment.infrastructure.persistence.jpa.repositories.DeliveryPersistenceRepository;
import com.halofuel.fuelbridge.platform.fulfillment.infrastructure.persistence.jpa.repositories.DriverPersistenceRepository;
import com.halofuel.fuelbridge.platform.fulfillment.infrastructure.persistence.jpa.repositories.VehiclePersistenceRepository;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;

/** Keeps scenarios independent by clearing deliveries, drivers, vehicles and the Equipment stand-in. */
public class CucumberHooks {

    @Autowired
    private DeliveryPersistenceRepository deliveries;

    @Autowired
    private DriverPersistenceRepository drivers;

    @Autowired
    private VehiclePersistenceRepository vehicles;

    @Autowired
    private EquipmentHttpStub equipmentService;

    @Before
    public void clearFulfillmentData() {
        deliveries.deleteAll();
        drivers.deleteAll();
        vehicles.deleteAll();
        equipmentService.reset();
    }
}
