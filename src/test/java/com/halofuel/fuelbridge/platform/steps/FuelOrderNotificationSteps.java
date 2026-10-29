package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;

import com.halofuel.fuelbridge.platform.ordering.application.commandservices.FuelOrderCommandService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.DispatchFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;

import com.halofuel.fuelbridge.platform.iam.infrastructure.tokens.jwt.BearerTokenService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FuelOrderNotificationSteps {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    @Autowired
    private FuelOrderCommandService fuelOrderCommandService;

    @Autowired
    private NotificationHttpStub notificationService;

    @Autowired
    private BearerTokenService tokenService;

    @Autowired
    private CucumberSpringConfiguration.DispatchEventRecorder eventRecorder;

    private User buyer;
    private FuelOrder fuelOrder;

    @Given("a buyer exists for company {int}")
    public void aBuyerExistsForCompany(int companyId) {

        eventRecorder.clear();
        notificationService.clear();

        String username = "buyer-test-" + UUID.randomUUID();

        User user = new User(username, "test-only-password");
        user.setCompanyId((long) companyId);

        buyer = userRepository.save(user);

        assertNotNull(buyer.getId());
        assertEquals((long) companyId, buyer.getCompanyId());
    }

    @Given("a fuel order exists for company {int} with status {string}")
    public void aFuelOrderExistsForCompany(int companyId, String status) {

        FuelOrder order = new FuelOrder();

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

        fuelOrderCommandService.handle(
                new DispatchFuelOrderCommand(fuelOrder.getId())
        );

        FuelOrder updatedOrder = fuelOrderRepository
                .findById(fuelOrder.getId())
                .orElseThrow();

        assertEquals(OrderStatus.DISPATCHED, updatedOrder.getStatus());
    }

    @Then("a {string} should be published")
    public void anEventShouldBePublished(String eventName) {

        assertEquals("FuelOrderDispatchedEvent", eventName);

        assertTrue(
                eventRecorder.wasPublished(
                        fuelOrder.getId(),
                        buyer.getCompanyId()
                ),
                "The dispatch event was not published"
        );
    }

    @Then("an {string} notification should be created for the buyer")
    public void aNotificationShouldBeCreated(String type) {

        var accepted = notificationService.accepted().stream()
                .filter(notification -> type.equals(notification.request().type())
                        && buyer.getId().equals(notification.request().userId())
                        && fuelOrder.getId().equals(notification.request().referenceId()))
                .findFirst().orElseThrow(() -> new AssertionError("The notification HTTP request was not accepted"));
        assertNotNull(accepted.authorization());
        assertTrue(accepted.authorization().startsWith("Bearer "));
        var jwt = accepted.authorization().substring(7);
        assertTrue(tokenService.validateToken(jwt));
        assertEquals("fuelbridge-notification-publisher", tokenService.getUsernameFromToken(jwt));
    }
}
