package com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.ProviderCompany;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.OrderSummaryResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ProviderCompanyDetailResource;
import com.halofuel.fuelbridge.platform.ordering.domain.model.aggregates.FuelOrder;

import java.util.List;

public class ProviderCompanyDetailResourceAssembler {

    public static ProviderCompanyDetailResource toResource(ProviderCompany company, List<FuelOrder> orders) {
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

        return new ProviderCompanyDetailResource(
                company.getId(),
                company.getName(),
                company.getRuc(),
                company.getRating(),
                company.getAddress(),
                company.getPhone(),
                company.getFuelTypesOffered(),
                company.getDescription(),
                orderSummaries
        );
    }
}