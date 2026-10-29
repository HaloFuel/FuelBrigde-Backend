package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.payment.domain.model.aggregates.Payment;
import com.halofuel.fuelbridge.platform.payment.domain.model.commands.CreatePaymentCommand;
import com.halofuel.fuelbridge.platform.payment.domain.model.valueobjects.PaymentMethod;
import com.halofuel.fuelbridge.platform.payment.domain.model.valueobjects.PaymentStatus;
import com.halofuel.fuelbridge.platform.payment.domain.repositories.PaymentRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class PaymentLifecycleSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment payment;
    private MvcResult response;

    @Given("a payment exists with status {string}")
    public void aPaymentExistsWithStatus(String status) {
        var command = new CreatePaymentCommand(4101L, 101L, 4800.0, PaymentMethod.BANK_TRANSFER);
        var newPayment = new Payment(command);
        switch (PaymentStatus.valueOf(status)) {
            case COMPLETED -> newPayment.complete("TXN-TEST-0001");
            case FAILED -> newPayment.fail();
            case REFUNDED -> {
                newPayment.complete("TXN-TEST-0001");
                newPayment.refund();
            }
            case PENDING -> { /* already PENDING after construction */ }
        }
        payment = paymentRepository.save(newPayment);
        assertNotNull(payment.getId());
    }

    @When("the operator requests a refund for the payment")
    public void theOperatorRequestsARefund() throws Exception {
        response = mockMvc.perform(
                post("/api/v1/payments/" + payment.getId() + "/refund").with(user("operator"))
        ).andReturn();
    }

    @When("the operator marks the payment as failed")
    public void theOperatorMarksThePaymentAsFailed() throws Exception {
        response = mockMvc.perform(
                post("/api/v1/payments/" + payment.getId() + "/fail").with(user("operator"))
        ).andReturn();
    }

    @Then("the payment response status code should be {int}")
    public void thePaymentResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getResponse().getStatus());
    }

    @Then("the payment status should be {string}")
    public void thePaymentStatusShouldBe(String expectedStatus) {
        var updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.valueOf(expectedStatus), updated.getStatus());
    }
}
