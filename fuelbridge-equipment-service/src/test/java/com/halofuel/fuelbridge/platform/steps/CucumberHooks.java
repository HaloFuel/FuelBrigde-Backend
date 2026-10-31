package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.equipment.infrastructure.persistence.jpa.repositories.EquipmentPersistenceRepository;

import io.cucumber.java.Before;

import org.springframework.beans.factory.annotation.Autowired;

/** Keeps scenarios independent by clearing persisted equipment before each one runs. */
public class CucumberHooks {

    @Autowired
    private EquipmentPersistenceRepository equipment;

    @Before
    public void clearEquipment() {
        equipment.deleteAll();
    }
}
