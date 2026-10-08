package com.halofuel.fuelbridge.platform.iam.domain.model.commands;

public record CreateBuyerCompanyCommand(
        String name,
        String ruc,
        String sector,
        String address,
        String contactEmail,
        String phone) {
}
