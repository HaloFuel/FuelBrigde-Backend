package com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.BuyerCompany;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.BuyerCompanyDetailResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.OrderSummaryResource;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;

import java.util.List;

public class BuyerCompanyDetailResourceAssembler {

    public static BuyerCompanyDetailResource toResource(BuyerCompany company, List<FuelOrder> orders) {
        List<OrderSummaryResource> orderSummaries = orders.stream()
                .map(order -> new OrderSummaryResource(
                        order.getId(),
                        order.getRequestedQuantity(),
                        order.getTotalPrice(),
                        order.getStatus().name(),
                        order.getDeliveryAddress(),
                        order.getScheduledDate()
                ))
                .toList();

        return new BuyerCompanyDetailResource(
                company.getId(),
                company.getName(),
                company.getRuc(),
                company.getSector(),
                company.getAddress(),
                company.getContactEmail(),
                company.getPhone(),
                orderSummaries
        );
    }
}