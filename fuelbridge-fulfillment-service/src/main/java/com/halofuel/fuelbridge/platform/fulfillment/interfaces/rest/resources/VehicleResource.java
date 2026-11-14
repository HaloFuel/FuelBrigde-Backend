package com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources;

public record VehicleResource(Long id, Long providerId, String licensePlate, String brand,
                              String model, Double capacity, String unit, String status) {
}
