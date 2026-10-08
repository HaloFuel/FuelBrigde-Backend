package com.halofuel.fuelbridge.platform.payment.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.payment.application.commandservices.PaymentCommandService;
import com.halofuel.fuelbridge.platform.payment.domain.model.aggregates.Payment;
import com.halofuel.fuelbridge.platform.payment.domain.model.commands.CompletePaymentCommand;
import com.halofuel.fuelbridge.platform.payment.domain.model.commands.CreatePaymentCommand;
import com.halofuel.fuelbridge.platform.payment.domain.model.commands.RefundPaymentCommand;
import com.halofuel.fuelbridge.platform.payment.domain.repositories.PaymentRepository;
import com.halofuel.fuelbridge.platform.ordering.domain.repositories.FuelOrderRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCommandServiceImpl implements PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final FuelOrderRepository fuelOrderRepository;

    public PaymentCommandServiceImpl(PaymentRepository paymentRepository,
                                     FuelOrderRepository fuelOrderRepository) {
        this.paymentRepository = paymentRepository;
        this.fuelOrderRepository = fuelOrderRepository;
    }

    @Override
    public Result<Payment, ApplicationError> handle(CreatePaymentCommand command) {
        var existing = paymentRepository.findByOrderId(command.orderId());
        if (existing.isPresent()) {
            return Result.failure(ApplicationError.conflict("Payment",
                    "A payment already exists for order " + command.orderId()));
        }
        var payment = new Payment(command);
        return Result.success(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public Result<Payment, ApplicationError> handle(CompletePaymentCommand command) {
        var existing = paymentRepository.findById(command.paymentId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Payment", command.paymentId().toString()));
        }
        var payment = existing.get();
        var order = fuelOrderRepository.findById(payment.getOrderId());
        if (order.isEmpty()) {
            return Result.failure(ApplicationError.notFound("FuelOrder", payment.getOrderId().toString()));
        }
        payment.complete(command.transactionReference());
        var paidOrder = order.get();
        paidOrder.markPaid();
        fuelOrderRepository.save(paidOrder);
        return Result.success(paymentRepository.save(payment));
    }

    @Override
    public Result<Payment, ApplicationError> handle(RefundPaymentCommand command) {
        var existing = paymentRepository.findById(command.paymentId());
        if (existing.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Payment", command.paymentId().toString()));
        }
        var payment = existing.get();
        payment.refund();
        return Result.success(paymentRepository.save(payment));
    }
}
