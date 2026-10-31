package com.halofuel.fuelbridge.platform.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.halofuel.fuelbridge.platform.inventory.domain.model.aggregates.FuelProduct;
import com.halofuel.fuelbridge.platform.inventory.domain.model.commands.CreateFuelProductCommand;
import com.halofuel.fuelbridge.platform.inventory.domain.model.valueobjects.FuelType;
import com.halofuel.fuelbridge.platform.inventory.domain.repositories.FuelProductRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class FuelProductInventorySteps {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuelProductRepository fuelProductRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FuelProduct product;
    private MvcResult response;
    private String productName;

    @Given("a valid new fuel product for inventory testing")
    public void aValidNewFuelProductForInventoryTesting() {
        productName = "Diesel Inventory BDD "
                + SEQUENCE.incrementAndGet();
    }

    @When("the provider registers the fuel product")
    public void theProviderRegistersTheFuelProduct() throws Exception {
        String body = """
                {
                  "name": "%s",
                  "fuelType": "DIESEL",
                  "pricePerUnit": 16.0,
                  "unit": "GAL",
                  "availableStock": 1000.0,
                  "capacity": 2000.0,
                  "providerId": 202,
                  "active": true
                }
                """.formatted(productName);

        response = mockMvc.perform(
                post("/api/v1/fuel-products")
                        .with(user("provider"))
                        .contentType(APPLICATION_JSON)
                        .content(body)
        ).andReturn();
    }

    @Then("the inventory creation response status should be {int}")
    public void inventoryCreationResponseStatus(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus,
                response.getResponse().getStatus());
    }

    @Then("the created inventory product should have {double} units of stock")
    public void createdInventoryProductShouldHaveStock(double expectedStock)
            throws Exception {

        var json = objectMapper.readTree(
                response.getResponse().getContentAsString()
        );

        long productId = json.path("id").asLong();
        assertTrue(productId > 0, "El producto debe tener un ID");

        var saved = fuelProductRepository.findById(productId)
                .orElseThrow();

        assertEquals(expectedStock, saved.getAvailableStock(), 0.001);
        assertEquals(productName, saved.getName());
    }

    @Given("a registered inventory product with {double} units of stock")
    public void aRegisteredInventoryProductWithStock(double stock) {

        var command = new CreateFuelProductCommand(
                "Diesel Inventory BDD " + SEQUENCE.incrementAndGet(),
                FuelType.DIESEL,
                16.0,
                "GAL",
                stock,
                2000.0,
                202L,
                true
        );

        product = fuelProductRepository.save(
                new FuelProduct(command)
        );

        assertNotNull(product.getId());
    }

    @When("the provider updates the inventory stock to {double} units")
    public void theProviderUpdatesInventoryStock(double newStock)
            throws Exception {

        String body = """
                {"newStock": %s}
                """.formatted(newStock);

        response = mockMvc.perform(
                post("/api/v1/fuel-products/"
                        + product.getId() + "/update-stock")
                        .with(user("provider"))
                        .contentType(APPLICATION_JSON)
                        .content(body)
        ).andReturn();
    }

    @Then("the inventory stock response status should be {int}")
    public void inventoryStockResponseStatus(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus,
                response.getResponse().getStatus());
    }

    @Then("the inventory product should retain {double} units of stock")
    public void inventoryProductShouldRetainStock(double expectedStock) {

        var updated = fuelProductRepository
                .findById(product.getId())
                .orElseThrow();

        assertEquals(expectedStock,
                updated.getAvailableStock(), 0.001);
    }


@When("the provider updates the inventory stock with a non-numeric value")
public void theProviderUpdatesInventoryStockWithNonNumericValue()
        throws Exception {

    String body = """
            {"newStock": "abc"}
            """;

    response = mockMvc.perform(
            post("/api/v1/fuel-products/"
                    + product.getId() + "/update-stock")
                    .with(user("provider"))
                    .contentType(APPLICATION_JSON)
                    .content(body)
    ).andReturn();
}


@Given("inventory product with id {int} does not exist")
public void inventoryProductDoesNotExist(int productId) {
    assertFalse(fuelProductRepository.existsById((long) productId));
}

@When("the provider updates a missing inventory product with id {int}")
public void theProviderUpdatesMissingInventoryProduct(int productId)
        throws Exception {

    String body = """
            {
              "name": "Diesel Test",
              "fuelType": "DIESEL",
              "pricePerUnit": 16.0,
              "unit": "GAL",
              "availableStock": 1000.0,
              "capacity": 2000.0,
              "active": true
            }
            """;

    response = mockMvc.perform(
            put("/api/v1/fuel-products/" + productId)
                    .with(user("provider"))
                    .contentType(APPLICATION_JSON)
                    .content(body)
    ).andReturn();
}

@When("the provider deletes a missing inventory product with id {int}")
public void theProviderDeletesMissingInventoryProduct(int productId)
        throws Exception {

    response = mockMvc.perform(
            delete("/api/v1/fuel-products/" + productId)
                    .with(user("provider"))
    ).andReturn();
}

@Then("the inventory missing product response status should be {int}")
public void inventoryMissingProductResponseStatus(int expectedStatus) {
    assertNotNull(response);
    assertEquals(
            expectedStatus,
            response.getResponse().getStatus()
    );
}



@When("the provider requests the registered inventory product by ID")
public void providerRequestsRegisteredInventoryProduct()
        throws Exception {

    response = mockMvc.perform(
            get("/api/v1/fuel-products/" + product.getId())
                    .with(user("provider"))
    ).andReturn();
}

@Then("the inventory lookup response status should be {int}")
public void inventoryLookupResponseStatus(int expectedStatus) {
    assertNotNull(response);
    assertEquals(expectedStatus,
            response.getResponse().getStatus());
}

@Then("the inventory lookup should match the registered product")
public void inventoryLookupShouldMatchRegisteredProduct()
        throws Exception {

    JsonNode json = objectMapper.readTree(
            response.getResponse().getContentAsString()
    );

    assertEquals(product.getId().longValue(),
            json.path("id").asLong());

    assertEquals(product.getAvailableStock(),
            json.path("availableStock").asDouble(), 0.001);
}

@Given("another inventory product belongs to provider {int}")
public void anotherInventoryProductBelongsToProvider(int providerId) {

    var command = new CreateFuelProductCommand(
            "Diesel Other Provider " + SEQUENCE.incrementAndGet(),
            FuelType.DIESEL,
            16.0,
            "GAL",
            500.0,
            2000.0,
            (long) providerId,
            true
    );

    var otherProduct = fuelProductRepository.save(
            new FuelProduct(command)
    );

    assertNotNull(otherProduct.getId());
}

@When("the provider requests inventory products for provider {int}")
public void providerRequestsInventoryByProvider(int providerId)
        throws Exception {

    response = mockMvc.perform(
            get("/api/v1/fuel-products/provider/" + providerId)
                    .with(user("provider"))
    ).andReturn();
}

@Then("the inventory provider lookup response status should be {int}")
public void inventoryProviderLookupStatus(int expectedStatus) {
    assertNotNull(response);
    assertEquals(expectedStatus,
            response.getResponse().getStatus());
}

@Then("the inventory provider lookup should contain only provider {int}")
public void inventoryProviderLookupContainsOnlyProvider(int providerId)
        throws Exception {

    JsonNode json = objectMapper.readTree(
            response.getResponse().getContentAsString()
    );

    assertTrue(json.isArray());
    assertTrue(json.size() > 0);

    for (JsonNode item : json) {
        assertEquals((long) providerId,
                item.path("providerId").asLong());
    }
}

@Then("the inventory provider lookup should include the registered product")
public void inventoryProviderLookupIncludesRegisteredProduct()
        throws Exception {

    JsonNode json = objectMapper.readTree(
            response.getResponse().getContentAsString()
    );

    boolean found = false;

    for (JsonNode item : json) {
        if (item.path("id").asLong() == product.getId()) {
            found = true;
            break;
        }
    }

    assertTrue(found,
            "El producto registrado debe aparecer en la consulta");
}

@When("the provider requests inventory product with id {int}")
public void providerRequestsInventoryProductWithId(int productId)
        throws Exception {

    response = mockMvc.perform(
            get("/api/v1/fuel-products/" + productId)
                    .with(user("provider"))
    ).andReturn();
}


@When("the provider requests the complete inventory product list")
public void providerRequestsCompleteInventoryList()
        throws Exception {

    response = mockMvc.perform(
            get("/api/v1/fuel-products")
                    .with(user("provider"))
    ).andReturn();
}

@Then("the inventory CRUD response status should be {int}")
public void inventoryCrudResponseStatus(int expectedStatus) {
    assertNotNull(response);
    assertEquals(expectedStatus,
            response.getResponse().getStatus());
}

@Then("the inventory list should include its registered product")
public void inventoryListShouldIncludeRegisteredProduct()
        throws Exception {

    JsonNode json = objectMapper.readTree(
            response.getResponse().getContentAsString()
    );

    assertTrue(json.isArray());

    boolean found = false;

    for (JsonNode item : json) {
        if (item.path("id").asLong()
                == product.getId().longValue()) {
            found = true;
            break;
        }
    }

    assertTrue(found,
            "El producto registrado debe aparecer en el listado");
}

@When("the provider updates the complete inventory product")
public void providerUpdatesCompleteInventoryProduct()
        throws Exception {

    String body = """
            {
              "name": "Diesel Inventory Updated",
              "fuelType": "DIESEL",
              "pricePerUnit": 17.5,
              "unit": "GAL",
              "availableStock": 1250.0,
              "capacity": 2500.0,
              "active": true
            }
            """;

    response = mockMvc.perform(
            put("/api/v1/fuel-products/" + product.getId())
                    .with(user("provider"))
                    .contentType(APPLICATION_JSON)
                    .content(body)
    ).andReturn();
}

@Then("the inventory product details should be updated")
public void inventoryProductDetailsShouldBeUpdated() {

    var updated = fuelProductRepository
            .findById(product.getId())
            .orElseThrow();

    assertEquals("Diesel Inventory Updated",
            updated.getName());

    assertEquals(17.5,
            updated.getPricePerUnit(), 0.001);

    assertEquals(1250.0,
            updated.getAvailableStock(), 0.001);

    assertEquals(2500.0,
            updated.getCapacity(), 0.001);

    assertEquals(202L,
            updated.getProviderId());
}

@When("the provider deletes the registered inventory product")
public void providerDeletesRegisteredInventoryProduct()
        throws Exception {

    response = mockMvc.perform(
            delete("/api/v1/fuel-products/" + product.getId())
                    .with(user("provider"))
    ).andReturn();
}

@Then("the deleted inventory product should no longer exist")
public void deletedInventoryProductShouldNoLongerExist() {

    assertFalse(
            fuelProductRepository.existsById(product.getId()),
            "El producto eliminado no debe existir"
    );
}


}
