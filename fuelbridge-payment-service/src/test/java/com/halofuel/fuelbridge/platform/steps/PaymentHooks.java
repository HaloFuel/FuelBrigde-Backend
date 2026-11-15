package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.payment.infrastructure.persistence.jpa.repositories.PaymentPersistenceRepository;

import io.cucumber.java.Before;

import org.springframework.beans.factory.annotation.Autowired;

/** Keeps scenarios independent: no stored payments and no recorded Ordering calls. */
public class PaymentHooks {

    @Autowired
    private PaymentPersistenceRepository payments;

    @Before
    public void resetState() {
        payments.deleteAll();
        OrderingHttpStub.reset();
    }
}
