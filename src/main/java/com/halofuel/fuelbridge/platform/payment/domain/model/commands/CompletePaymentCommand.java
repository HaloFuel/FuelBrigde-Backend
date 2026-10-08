package com.halofuel.fuelbridge.platform.payment.domain.model.commands;

public record CompletePaymentCommand(Long paymentId, String transactionReference) {
}
