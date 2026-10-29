package com.halofuel.fuelbridge.platform.notification.application.outboundservices;

import java.util.Optional;

public interface RecipientDirectory {
    Optional<Long> findByCompanyId(Long companyId);
    Optional<Long> findByProviderId(Long providerId);
}
