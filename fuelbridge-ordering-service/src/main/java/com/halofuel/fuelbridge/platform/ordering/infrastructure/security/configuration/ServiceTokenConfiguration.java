package com.halofuel.fuelbridge.platform.ordering.infrastructure.security.configuration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the shared-kernel issuer of the short-lived JWT that Ordering sends to the
 * Inventory, Notification and Identity &amp; Access services. They validate it locally
 * with the same shared secret.
 */
@Configuration
public class ServiceTokenConfiguration {

    public static final String SERVICE_NAME = "fuelbridge-ordering-service";

    @Bean
    public ServiceTokenIssuer serviceTokenIssuer(@Value("${authorization.jwt.secret}") String secret) {
        return new ServiceTokenIssuer(secret);
    }
}
