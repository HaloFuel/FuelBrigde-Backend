package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.inventory.domain.model.aggregates.FuelProduct;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.CreateFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.valueobjects.FuelType;
import com.halofuel.fuelbridge.platform.inventory.domain.repositories.FuelProductRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class FuelStockReservationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuelProductRepository fuelProductRepository;

    private FuelProduct product;
    private MvcResult response;

    @Given("a fuel product with {int} units of available stock")
    public void aFuelProductWithUnitsOfAvailableStock(int stock) {
        product = fuelProductRepository.save(new FuelProduct(new CreateFuelProductCommand(
                "Diesel B5 reserva", FuelType.DIESEL, 16.0, "GAL", (double) stock, 20000.0, 202L, true)));
    }

    @When("{int} units are reserved for a delivery")
    public void unitsAreReservedForADelivery(int quantity) throws Exception {
        response = send("/stock-reservations", quantity);
    }

    @When("{int} units are released from a cancelled reservation")
    public void unitsAreReleasedFromACancelledReservation(int quantity) throws Exception {
        response = send("/stock-releases", quantity);
    }

    @Then("the stock reservation response status should be {int}")
    public void theStockReservationResponseStatusShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the fuel product should have {int} units of available stock")
    public void theFuelProductShouldHaveUnitsOfAvailableStock(int expectedStock) {
        var stored = fuelProductRepository.findById(product.getId()).orElseThrow();
        assertEquals((double) expectedStock, stored.getAvailableStock());
    }

    private MvcResult send(String operation, int quantity) throws Exception {
        return mockMvc.perform(
                post("/api/v1/fuel-products/" + product.getId() + operation)
                        .with(user("fuelbridge-fulfillment"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"quantity\": " + quantity + "}")
        ).andReturn();
    }
}
