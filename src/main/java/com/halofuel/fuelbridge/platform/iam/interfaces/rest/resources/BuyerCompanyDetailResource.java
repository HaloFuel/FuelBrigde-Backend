package com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources;

import java.util.List;

public record BuyerCompanyDetailResource(
        Long id,
        String name,
        String ruc,
        String sector,
        String address,
        String contactEmail,
        String phone,
        List<OrderSummaryResource> orders
) {}