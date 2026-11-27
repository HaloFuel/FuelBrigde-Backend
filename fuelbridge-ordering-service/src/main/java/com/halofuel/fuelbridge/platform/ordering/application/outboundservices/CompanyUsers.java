package com.halofuel.fuelbridge.platform.ordering.application.outboundservices;

import java.util.Optional;

/**
 * Outbound port through which Ordering finds the user of a buyer company in the
 * Identity &amp; Access bounded context, to address the order notifications. Replaces the
 * direct use of the IAM user repository that existed inside the FuelBridge Platform.
 */
public interface CompanyUsers {

    /**
     * @return the identifier of the company's user, or empty when IAM does not know one
     * @throws IllegalStateException when Identity &amp; Access cannot be reached
     */
    Optional<Long> findUserIdByCompanyId(Long companyId);
}
