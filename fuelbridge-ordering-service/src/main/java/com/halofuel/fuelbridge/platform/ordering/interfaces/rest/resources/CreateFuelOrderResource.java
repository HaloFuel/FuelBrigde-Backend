package com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources;

import java.time.LocalDate;

public record CreateFuelOrderResource(Long companyId, Long providerId, Long fuelProductId,
                                      Long equipmentId, Double requestedQuantity,
                                      String deliveryAddress, LocalDate scheduledDate) {
}
