package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.application.commandservices.FuelOrderCommandService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CancelFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.DispatchFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.security.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Steps for fuel-order-notification.feature (US-30): the buyer's user is resolved in
 * Identity &amp; Access and the notification is sent to the Notification service with
 * Ordering's service token.
 */
public class FuelOrderNotificationSteps {

    private static final long BUYER_USER_ID = 4242L;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    @Autowired
    private FuelOrderCommandService fuelOrderCommandService;

    @Autowired
    private NotificationHttpStub notificationService;

    @Autowired
    private IamHttpStub iamService;

    @Autowired
    private BearerTokenService tokenService;

    @Autowired
    private CucumberSpringConfiguration.DispatchEventRecorder eventRecorder;

    private long buyerCompanyId;
    private FuelOrder fuelOrder;

    @Given("a buyer exists for company {int}")
    public void aBuyerExistsForCompany(int companyId) {
        buyerCompanyId = companyId;
        iamService.registerUser(buyerCompanyId, BUYER_USER_ID);
    }

    @Given("a fuel order exists for company {int} with status {string}")
    public void aFuelOrderExistsForCompany(int companyId, String status) {
        var order = new FuelOrder();
        order.setCompanyId((long) companyId);
        order.setProviderId(202L);
        order.setFuelProductId(303L);
        order.setRequestedQuantity(300.0);
        order.setTotalPrice(1500.0);
        order.setDeliveryAddress("Test delivery address");
        order.setScheduledDate(LocalDate.now().plusDays(1));
        order.setStatus(OrderStatus.valueOf(status));
        fuelOrder = fuelOrderRepository.save(order);
        assertNotNull(fuelOrder.getId());
    }

    @When("the fuel order is dispatched")
    public void theFuelOrderIsDispatched() {
        fuelOrderCommandService.handle(new DispatchFuelOrderCommand(fuelOrder.getId()));
        var updatedOrder = fuelOrderRepository.findById(fuelOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.DISPATCHED, updatedOrder.getStatus());
    }

    @When("the buyer tries to cancel the fuel order")
    public void theBuyerTriesToCancelTheFuelOrder() {
        assertThrows(IllegalStateException.class,
                () -> fuelOrderCommandService.handle(new CancelFuelOrderCommand(fuelOrder.getId())));
    }

    @Then("the fuel order status remains {string}")
    public void theFuelOrderStatusRemains(String status) {
        var stored = fuelOrderRepository.findById(fuelOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.valueOf(status), stored.getStatus());
    }

    @Then("a {string} should be published")
    public void anEventShouldBePublished(String eventName) {
        assertEquals("FuelOrderDispatchedEvent", eventName);
        assertTrue(eventRecorder.wasPublished(fuelOrder.getId(), buyerCompanyId),
                "The dispatch event was not published");
    }

    @Then("an {string} notification should be created for the buyer")
    public void aNotificationShouldBeCreated(String type) {
        var accepted = notificationService.accepted().stream()
                .filter(notification -> type.equals(notification.request().type())
                        && BUYER_USER_ID == notification.request().userId()
                        && fuelOrder.getId().equals(notification.request().referenceId()))
                .findFirst().orElseThrow(() -> new AssertionError("The notification HTTP request was not accepted"));
        assertNotNull(accepted.authorization());
        assertTrue(accepted.authorization().startsWith("Bearer "));
        var jwt = accepted.authorization().substring(7);
        assertTrue(tokenService.validateToken(jwt));
        assertEquals("fuelbridge-ordering-service", tokenService.getUsernameFromToken(jwt));
    }

    @Then("no notification should be sent")
    public void noNotificationShouldBeSent() {
        assertTrue(notificationService.accepted().isEmpty());
    }
}
