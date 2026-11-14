package com.halofuel.fuelbridge.platform.fulfillment.infrastructure.security.configuration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the shared-kernel issuer of the short-lived JWT that Fulfillment sends
 * to the Inventory and Equipment services. Both validate it locally with the same
 * shared secret, so no call to Identity &amp; Access is needed.
 */
@Configuration
public class ServiceTokenConfiguration {

    public static final String SERVICE_NAME = "fuelbridge-fulfillment-service";

    @Bean
    public ServiceTokenIssuer serviceTokenIssuer(@Value("${authorization.jwt.secret}") String secret) {
        return new ServiceTokenIssuer(secret);
    }
}
