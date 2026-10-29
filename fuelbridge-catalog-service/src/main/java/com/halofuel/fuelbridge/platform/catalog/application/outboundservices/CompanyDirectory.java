package com.halofuel.fuelbridge.platform.catalog.application.outboundservices;

public interface CompanyDirectory {
    boolean buyerExists(Long companyId);
    boolean providerExists(Long providerId);
}
