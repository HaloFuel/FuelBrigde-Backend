package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class FuelOrderDispatchSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    private FuelOrder fuelOrder;
    private MvcResult response;
    private boolean authenticated;

    @Given("a fuel order exists with status {string}")
    public void aFuelOrderExistsWithStatus(String status) {

        // Crear un pedido de prueba
        FuelOrder order = new FuelOrder();

        order.setCompanyId(101L);
        order.setProviderId(202L);
        order.setFuelProductId(303L);
        order.setRequestedQuantity(300.0);
        order.setTotalPrice(1500.0);
        order.setDeliveryAddress("Test delivery address");
        order.setScheduledDate(LocalDate.now().plusDays(1));
        order.setStatus(OrderStatus.valueOf(status));

        // Guardar el pedido en H2
        fuelOrder = fuelOrderRepository.save(order);

        assertNotNull(fuelOrder.getId());
    }

    @Given("the provider is authenticated")
    public void theProviderIsAuthenticated() {

        // La autenticacion sera simulada por Spring Security Test
        authenticated = true;
    }

    @When("the provider sends a POST request to {string}")
    public void theProviderSendsPostRequest(String endpoint) throws Exception {

        assertTrue(authenticated);

        // Reemplazar orderId por el ID del pedido creado
        String url = endpoint.replace(
                "{orderId}",
                fuelOrder.getId().toString()
        );

        // Ejecutar la peticion HTTP de prueba
        response = mockMvc.perform(
                post(url).with(user("provider").roles("PROVIDER"))
        ).andReturn();
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {

        assertNotNull(response);

        assertEquals(
                expectedStatus,
                response.getResponse().getStatus()
        );
    }

    @Then("the fuel order status should be {string}")
    public void theFuelOrderStatusShouldBe(String expectedStatus) {

        // Consultar nuevamente el pedido guardado
        FuelOrder updatedOrder = fuelOrderRepository
                .findById(fuelOrder.getId())
                .orElseThrow();

        // Comprobar el cambio de estado
        assertEquals(
                OrderStatus.valueOf(expectedStatus),
                updatedOrder.getStatus()
        );
    }
}
