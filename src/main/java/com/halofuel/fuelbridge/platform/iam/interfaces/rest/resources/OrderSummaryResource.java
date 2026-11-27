package com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources;

import java.time.LocalDate;

public record OrderSummaryResource(
        Long id,
        Double requestedQuantity,
        Double totalPrice,
        String status,
        String deliveryAddress,
        LocalDate scheduledDate
) {}