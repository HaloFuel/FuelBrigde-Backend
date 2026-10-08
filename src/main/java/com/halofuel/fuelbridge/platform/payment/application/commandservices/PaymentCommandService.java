package com.halofuel.fuelbridge.platform.payment.application.commandservices;

import com.halofuel.fuelbridge.platform.payment.domain.model.aggregates.Payment;
import com.halofuel.fuelbridge.platform.payment.domain.model.commands.CompletePaymentCommand;
import com.halofuel.fuelbridge.platform.payment.domain.model.commands.CreatePaymentCommand;
import com.halofuel.fuelbridge.platform.payment.domain.model.commands.RefundPaymentCommand;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;

public interface PaymentCommandService {
    Result<Payment, ApplicationError> handle(CreatePaymentCommand command);
    Result<Payment, ApplicationError> handle(CompletePaymentCommand command);
    Result<Payment, ApplicationError> handle(RefundPaymentCommand command);
}
