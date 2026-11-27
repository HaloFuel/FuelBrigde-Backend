package com.halofuel.fuelbridge.platform.equipment.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.equipment.domain.model.commands.CreateEquipmentCommand;
import com.halofuel.fuelbridge.platform.equipment.domain.model.valueobjects.EquipmentType;
import com.halofuel.fuelbridge.platform.equipment.domain.model.valueobjects.FuelType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests of the fuel-level rules of the Equipment aggregate. */
class EquipmentTest {

    private static Equipment equipmentAt(Double currentLevel) {
        return new Equipment(new CreateEquipmentCommand("Excavadora CAT 320", EquipmentType.EXCAVATOR,
                "EQ-320-LIM", FuelType.DIESEL, 400.0, currentLevel, "Obra Villa El Salvador, Lima",
                "ACTIVE", true, 25, "2026-10-08", 101L, null));
    }

    @Test
    void receiveFuelAddsTheDeliveredQuantity() {
        var equipment = equipmentAt(80.0);
        equipment.receiveFuel(240.0);
        assertEquals(320.0, equipment.getCurrentLevel());
    }

    @Test
    void receiveFuelNeverExceedsTheTankCapacity() {
        var equipment = equipmentAt(80.0);
        equipment.receiveFuel(500.0);
        assertEquals(400.0, equipment.getCurrentLevel());
    }

    @Test
    void receiveFuelStartsFromZeroWhenTheLevelIsUnknown() {
        var equipment = equipmentAt(null);
        equipment.receiveFuel(100.0);
        assertEquals(100.0, equipment.getCurrentLevel());
    }

    @Test
    void receiveFuelRejectsNonPositiveOrMissingQuantities() {
        var equipment = equipmentAt(80.0);
        for (Double quantity : new Double[] {null, 0.0, -10.0, Double.NaN}) {
            var error = assertThrows(IllegalArgumentException.class, () -> equipment.receiveFuel(quantity));
            assertEquals("Received fuel quantity must be greater than zero", error.getMessage());
        }
        assertEquals(80.0, equipment.getCurrentLevel());
    }

    @Test
    void assignFavoriteProviderStoresTheProvider() {
        var equipment = equipmentAt(80.0);
        equipment.assignFavoriteProvider(202L);
        assertEquals(202L, equipment.getFavoriteProviderId());
    }
}
