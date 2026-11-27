package com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources;

import java.util.List;

public record ProviderCompanyDetailResource(
        Long id,
        String name,
        String ruc,
        Double rating,
        String address,
        String phone,
        List<String> fuelTypesOffered,
        String description,
        List<OrderSummaryResource> orders
) {}