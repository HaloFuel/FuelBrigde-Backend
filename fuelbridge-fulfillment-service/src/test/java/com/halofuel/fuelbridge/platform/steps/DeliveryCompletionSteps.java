package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Delivery;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Driver;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Vehicle;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CreateDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DeliveryRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DriverRepository;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.VehicleRepository;
import com.halofuel.fuelbridge.platform.fulfillment.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Steps for delivery-completion.feature: completing a delivery registers the fuel in
 * Equipment and requests the receive transition to Ordering, relaying the caller's JWT.
 */
public class DeliveryCompletionSteps {

    private static final AtomicInteger FIXTURE_SEQUENCE = new AtomicInteger(500);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BearerTokenService tokenService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrderingHttpStub orderingService;

    @Autowired
    private EquipmentHttpStub equipmentService;

    private Delivery delivery;
    private String bearerToken;
    private MvcResult response;

    @Given("a dispatched delivery of provider {int} for an order of {double} units for equipment {long}")
    public void aDispatchedDeliveryForEquipment(int providerId, double quantity, long equipmentId) {
        var suffix = FIXTURE_SEQUENCE.incrementAndGet();
        var driver = driverRepository.save(new Driver((long) providerId, "Rosa", "Mamani",
                "M7812" + suffix, "987654321", "rosa.mamani+" + suffix + "@example.com", "ASSIGNED"));
        var vehicle = vehicleRepository.save(new Vehicle((long) providerId, "CKD-" + suffix, "Scania",
                "R450", 15000.0, "L", "IN_ROUTE"));
        equipmentService.addEquipment(equipmentId);
        var orderId = orderingService.addOrder(providerId, 303L, equipmentId, quantity, "DISPATCHED");
        var scheduled = new Delivery(new CreateDeliveryCommand(orderId, (long) providerId, driver.getId(),
                vehicle.getId(), LocalDate.now().plusDays(1).toString(), null));
        scheduled.dispatch();
        delivery = deliveryRepository.save(scheduled);
    }

    @Given("the Equipment service is unavailable")
    public void theEquipmentServiceIsUnavailable() {
        equipmentService.setAvailable(false);
    }

    @Given("the order of that delivery is {string} in Ordering")
    public void theOrderOfThatDeliveryIs(String status) {
        orderingService.setStatus(delivery.getOrderId(), status);
    }

    @When("the provider completes that delivery with a valid token")
    public void theProviderCompletesThatDelivery() throws Exception {
        complete(delivery.getId());
    }

    @When("the provider completes delivery {long} with a valid token")
    public void theProviderCompletesDelivery(long deliveryId) throws Exception {
        complete(deliveryId);
    }

    private void complete(Long deliveryId) throws Exception {
        bearerToken = "Bearer " + tokenService.generateToken("provider.andina");
        response = mockMvc.perform(post("/api/v1/deliveries/{deliveryId}/complete", deliveryId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)).andReturn();
    }

    @Then("the completion response status code should be {int}")
    public void theCompletionResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the completed delivery status should be {string}")
    public void theCompletedDeliveryStatusShouldBe(String expectedStatus) {
        var stored = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertEquals(expectedStatus, stored.getStatus().name());
    }

    @Then("the order of that delivery should be {string} in Ordering")
    public void theOrderOfThatDeliveryShouldBe(String expectedStatus) {
        assertEquals(expectedStatus, orderingService.statusOf(delivery.getOrderId()));
    }

    @Then("equipment {long} should have received {double} units")
    public void equipmentShouldHaveReceivedUnits(long equipmentId, double expectedQuantity) {
        assertEquals(expectedQuantity, equipmentService.receivedBy(equipmentId));
    }

    @Then("the driver and vehicle of that delivery should be {string}")
    public void theDriverAndVehicleShouldBe(String expectedStatus) {
        assertEquals(expectedStatus, driverRepository.findById(delivery.getDriverId()).orElseThrow().getStatus());
        assertEquals(expectedStatus, vehicleRepository.findById(delivery.getVehicleId()).orElseThrow().getStatus());
    }

    @Then("Ordering should have received the provider token")
    public void orderingShouldHaveReceivedTheProviderToken() {
        assertEquals(bearerToken, orderingService.lastAuthorization());
    }
}
