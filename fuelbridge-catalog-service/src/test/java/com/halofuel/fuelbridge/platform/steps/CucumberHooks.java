package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.catalog.infrastructure.persistence.jpa.repositories.ProviderRatingPersistenceRepository;

import io.cucumber.java.Before;

import org.springframework.beans.factory.annotation.Autowired;

/** Keeps scenarios independent by clearing persisted ratings before each one runs. */
public class CucumberHooks {

    @Autowired
    private ProviderRatingPersistenceRepository ratings;

    @Before
    public void clearRatings() {
        ratings.deleteAll();
    }
}
