package com.halofuel.fuelbridge.platform.steps;

import io.cucumber.java.After;

/** Keeps scenarios independent by resetting the simulated upstream outage after each one. */
public class CucumberHooks {

    @After
    public void resetUpstreamAvailability() {
        AnalyticsSourceStub.UNAVAILABLE.set(false);
    }
}
