package com.halofuel.fuelbridge.platform.equipment.interfaces.rest.resources;

import com.halofuel.fuelbridge.platform.equipment.domain.model.valueobjects.EquipmentType;
import com.halofuel.fuelbridge.platform.inventory.domain.model.valueobjects.FuelType;

public record EquipmentResource(Long id, String name, EquipmentType equipmentType, String licensePlate,
                                FuelType fuelType, Double tankCapacity, Double currentLevel,String location, String status, Boolean autoRefill,
                                Integer refillThreshold, String lastRefillDate, Long companyId,
                                Long favoriteProviderId) {
}
