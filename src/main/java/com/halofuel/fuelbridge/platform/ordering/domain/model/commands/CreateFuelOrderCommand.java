package com.halofuel.fuelbridge.platform.ordering.domain.model.commands;

import java.time.LocalDate;

public record CreateFuelOrderCommand(
        Long companyId,
        Long providerId,
        Long fuelProductId,
        Long equipmentId,
        Double requestedQuantity,
        String deliveryAddress,
        LocalDate scheduledDate) {
}
