package com.halofuel.fuelbridge.platform.equipment.domain.model.valueobjects;

/**
 * Fuel accepted by a piece of equipment. Equipment keeps its own copy instead of
 * importing the Inventory enum, so the bounded contexts share only the textual value.
 */
public enum FuelType {
    DIESEL,
    GASOLINE,
    GASOLINE_84,
    GASOLINE_90,
    GASOLINE_95,
    GASOLINE_97,
    GLP,
    GNV
}
