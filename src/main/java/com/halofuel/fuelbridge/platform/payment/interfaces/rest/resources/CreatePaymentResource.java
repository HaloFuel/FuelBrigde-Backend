package com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources;

import com.halofuel.fuelbridge.platform.payment.domain.model.valueobjects.PaymentMethod;

public record CreatePaymentResource(Long orderId, Long companyId, Double amount, PaymentMethod paymentMethod) {
}
