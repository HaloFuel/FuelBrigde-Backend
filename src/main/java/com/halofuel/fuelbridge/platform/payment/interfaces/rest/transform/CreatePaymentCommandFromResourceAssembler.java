package com.halofuel.fuelbridge.platform.payment.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.payment.domain.model.commands.CreatePaymentCommand;
import com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.CreatePaymentResource;

public final class CreatePaymentCommandFromResourceAssembler {

    private CreatePaymentCommandFromResourceAssembler() {
    }

    public static CreatePaymentCommand toCommandFromResource(CreatePaymentResource resource) {
        return new CreatePaymentCommand(resource.orderId(), resource.companyId(),
                resource.amount(), resource.paymentMethod());
    }
}
