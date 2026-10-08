package com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources;

public record CreateBuyerCompanyResource(String name, String ruc, String sector,
                                         String address, String contactEmail, String phone) {
}
