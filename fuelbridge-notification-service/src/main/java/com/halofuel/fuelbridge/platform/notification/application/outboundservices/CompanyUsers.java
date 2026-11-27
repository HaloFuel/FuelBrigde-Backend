package com.halofuel.fuelbridge.platform.notification.application.outboundservices;

import java.util.Optional;

/**
 * Outbound port through which Notification finds the user of a buyer company in the
 * Identity &amp; Access bounded context, to address an order event received off the
 * "order-events" broker (T10.04a), where there is no inbound caller JWT to resolve the
 * recipient through {@link RecipientDirectory} as the REST endpoints do.
 */
public interface CompanyUsers {

    /**
     * @return the identifier of the company's user, or empty when IAM does not know one
     * @throws IllegalStateException when Identity &amp; Access cannot be reached
     */
    Optional<Long> findUserIdByCompanyId(Long companyId);
}
