package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Delivery;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Driver;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Vehicle;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CreateDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DeliveryRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DriverRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.VehicleRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class DispatchAssignmentSteps {

    // Driver license numbers and vehicle plates are unique columns; scenarios
    // share the same H2 instance, so each fixture needs a fresh suffix.
    private static final AtomicInteger FIXTURE_SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private InventoryHttpStub inventoryService;

    @Autowired
    private OrderingHttpStub orderingService;

    private Driver driver;
    private Vehicle vehicle;
    private Long productId;
    private Long orderId;
    private Long orderProviderId;
    private MvcResult response;

    @Given("an available driver and an available vehicle for provider {int}")
    public void anAvailableDriverAndVehicle(int providerId) {
        createDriverAndVehicle(providerId, "AVAILABLE", "AVAILABLE");
    }

    @Given("an available driver and an unavailable vehicle for provider {int}")
    public void anAvailableDriverAndUnavailableVehicle(int providerId) {
        createDriverAndVehicle(providerId, "AVAILABLE", "IN_ROUTE");
    }

    private void createDriverAndVehicle(int providerId, String driverStatus, String vehicleStatus) {
        var suffix = FIXTURE_SEQUENCE.incrementAndGet();
        driver = driverRepository.save(new Driver((long) providerId, "Juan", "Perez",
                "Q1234" + suffix, "999111222", "juan.perez+" + suffix + "@example.com", driverStatus));
        vehicle = vehicleRepository.save(new Vehicle((long) providerId, "ABC-" + suffix, "Volvo",
                "FH", 500.0, "GAL", vehicleStatus));
    }

    @Given("a pending fuel order for provider {int} requesting {double} units")
    public void aPendingFuelOrderRequestingUnits(int providerId, double quantity) {
        aFuelOrderWithStatus(providerId, quantity, "PENDING");
    }

    @Given("a fuel order for provider {int} requesting {double} units with status {string}")
    public void aFuelOrderWithStatus(int providerId, double quantity, String status) {
        ensureProduct();
        // Ordering remains in the FuelBridge Platform; the scenario uses its HTTP stand-in.
        orderProviderId = (long) providerId;
        orderId = orderingService.addOrder(providerId, productId, null, quantity, status);
    }

    @Given("the fuel product has {double} units of available stock")
    public void theFuelProductHasUnitsOfAvailableStock(double stock) {
        ensureProduct();
        inventoryService.setStock(productId, stock);
    }

    @Given("that order already has a scheduled delivery")
    public void thatOrderAlreadyHasAScheduledDelivery() {
        var suffix = FIXTURE_SEQUENCE.incrementAndGet();
        var existingDriver = driverRepository.save(new Driver(orderProviderId, "Otro", "Conductor",
                "Q9999" + suffix, "999000111", "otro+" + suffix + "@example.com", "AVAILABLE"));
        var existingVehicle = vehicleRepository.save(new Vehicle(orderProviderId, "XYZ-" + suffix, "Scania",
                "P410", 500.0, "GAL", "AVAILABLE"));
        var command = new CreateDeliveryCommand(orderId, orderProviderId,
                existingDriver.getId(), existingVehicle.getId(), LocalDate.now().plusDays(2).toString(), null);
        var delivery = new Delivery(command);
        delivery.dispatch();
        deliveryRepository.save(delivery);
    }

    @Given("Ordering will reject the dispatch of that order")
    public void orderingWillRejectTheDispatchOfThatOrder() {
        orderingService.rejectDispatchOf(orderId);
    }

    private void ensureProduct() {
        if (productId != null) {
            return;
        }
        // Inventory is a standalone service; the scenario uses its HTTP stand-in.
        productId = inventoryService.addProduct("Diesel B5", 16.0, 10000.0, 202L);
    }

    @When("the provider schedules a delivery assigning that driver and vehicle")
    public void theProviderSchedulesADelivery() throws Exception {
        var body = """
                {"orderId": %d, "providerId": %d, "driverId": %d, "vehicleId": %d,
                 "scheduledDate": "%s", "notes": "Entrega de prueba BDD"}
                """.formatted(orderId, orderProviderId, driver.getId(), vehicle.getId(),
                LocalDate.now().plusDays(2));
        response = mockMvc.perform(
                post("/api/v1/deliveries").with(user("provider"))
                        .contentType(APPLICATION_JSON)
                        .content(body)
        ).andReturn();
    }

    @Then("the delivery response status code should be {int}")
    public void theDeliveryResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the delivery status should be {string}")
    public void theDeliveryStatusShouldBe(String expectedStatus) {
        var delivery = deliveryRepository.findByOrderId(orderId).orElseThrow();
        assertEquals(expectedStatus, delivery.getStatus().name());
    }

    @Then("no delivery should exist for that order")
    public void noDeliveryShouldExistForThatOrder() {
        assertTrue(deliveryRepository.findByOrderId(orderId).isEmpty());
    }

    @Then("the assigned driver status should be {string}")
    public void theAssignedDriverStatusShouldBe(String expectedStatus) {
        var updated = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(expectedStatus, updated.getStatus());
    }

    @Then("the assigned vehicle status should be {string}")
    public void theAssignedVehicleStatusShouldBe(String expectedStatus) {
        var updated = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(expectedStatus, updated.getStatus());
    }

    @Then("the available stock should be {double} units")
    public void theAvailableStockShouldBeUnits(double expectedStock) {
        assertEquals(expectedStock, inventoryService.stockOf(productId));
    }

    @Then("the order status in Ordering should be {string}")
    public void theOrderStatusInOrderingShouldBe(String expectedStatus) {
        assertEquals(expectedStatus, orderingService.statusOf(orderId));
    }
}
