package com.halofuel.fuelbridge.platform.payment.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.payment.domain.model.commands.CreatePaymentCommand;
import com.halofuel.fuelbridge.platform.payment.domain.model.valueobjects.PaymentMethod;
import com.halofuel.fuelbridge.platform.payment.domain.model.valueobjects.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @summary Pruebas unitarias del agregado Payment.
 * @remarks Verifica los datos y el estado inicial del constructor, así como los cambios
 * de estado al completar, reembolsar y fallar un pago, sin contexto de Spring ni mocks.
 * @author HaloFuel
 */
class PaymentTest {

    @Test
    void constructorCopiesCommandDataAndInitializesPendingPayment() {
        // Arrange
        var command = new CreatePaymentCommand(10L, 20L, 150.50, PaymentMethod.BANK_TRANSFER);

        // Act
        var payment = new Payment(command);

        // Assert
        assertThat(payment.getOrderId()).isEqualTo(command.orderId());
        assertThat(payment.getCompanyId()).isEqualTo(command.companyId());
        assertThat(payment.getAmount()).isEqualTo(command.amount());
        assertThat(payment.getPaymentMethod()).isEqualTo(command.paymentMethod());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getId()).isNull();
        assertThat(payment.getTransactionReference()).isNull();
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void completeMarksThePaymentAsCompletedAndRecordsReferenceAndPaymentTime() {
        // Arrange
        var command = new CreatePaymentCommand(10L, 20L, 150.50, PaymentMethod.BANK_TRANSFER);
        var payment = new Payment(command);
        var transactionReference = "TXN-12345";
        var beforeCompletion = LocalDateTime.now();

        // Act
        payment.complete(transactionReference);

        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactionReference()).isEqualTo(transactionReference);
        assertThat(payment.getPaidAt()).isBetween(beforeCompletion, LocalDateTime.now());
    }

    @Test
    void refundMarksThePaymentAsRefunded() {
        // Arrange
        var command = new CreatePaymentCommand(10L, 20L, 150.50, PaymentMethod.BANK_TRANSFER);
        var payment = new Payment(command);
        payment.complete("TXN-12345");

        // Act
        payment.refund();

        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void failMarksThePaymentAsFailed() {
        // Arrange
        var command = new CreatePaymentCommand(10L, 20L, 150.50, PaymentMethod.BANK_TRANSFER);
        var payment = new Payment(command);

        // Act
        payment.fail();

        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}
