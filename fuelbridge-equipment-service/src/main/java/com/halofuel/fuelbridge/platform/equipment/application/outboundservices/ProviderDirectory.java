package com.halofuel.fuelbridge.platform.equipment.application.outboundservices;

/**
 * Outbound port that tells Equipment whether a provider company exists.
 * Provider companies belong to Identity &amp; Access, so Equipment asks for them
 * instead of reading another bounded context's data.
 */
public interface ProviderDirectory {
    boolean providerExists(Long providerId);
}
