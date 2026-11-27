package com.halofuel.fuelbridge.platform.inventory.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.CreateFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.valueobjects.FuelType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests of the stock rules of the FuelProduct aggregate. */
class FuelProductTest {

    private static FuelProduct productWith(double stock) {
        return new FuelProduct(new CreateFuelProductCommand("Diésel B5", FuelType.DIESEL, 16.5, "GAL",
                stock, 20000.0, 202L, true));
    }

    @Test
    void reserveStockWithdrawsTheRequestedQuantity() {
        var product = productWith(1000.0);
        assertTrue(product.reserveStock(300.0));
        assertEquals(700.0, product.getAvailableStock());
    }

    @Test
    void reserveStockLeavesTheStockUnchangedWhenItIsInsufficient() {
        var product = productWith(200.0);
        assertFalse(product.reserveStock(300.0));
        assertEquals(200.0, product.getAvailableStock());
    }

    @Test
    void releaseStockReturnsAReservedQuantity() {
        var product = productWith(700.0);
        product.releaseStock(300.0);
        assertEquals(1000.0, product.getAvailableStock());
    }

    @Test
    void stockOperationsRejectNonPositiveOrMissingQuantities() {
        var product = productWith(1000.0);
        for (Double quantity : new Double[] {null, 0.0, -10.0, Double.NaN}) {
            assertThrows(IllegalArgumentException.class, () -> product.reserveStock(quantity));
            assertThrows(IllegalArgumentException.class, () -> product.releaseStock(quantity));
        }
        assertEquals(1000.0, product.getAvailableStock());
    }

    @Test
    void updateStockRejectsNegativeValues() {
        var product = productWith(1000.0);
        assertThrows(IllegalArgumentException.class, () -> product.updateStock(-1.0));
        assertEquals(1000.0, product.getAvailableStock());
    }
}
