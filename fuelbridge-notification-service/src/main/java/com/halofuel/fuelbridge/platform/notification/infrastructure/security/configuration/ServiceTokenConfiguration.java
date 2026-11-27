package com.halofuel.fuelbridge.platform.notification.infrastructure.security.configuration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the shared-kernel issuer of the short-lived JWT that Notification sends to
 * Identity &amp; Access when it resolves a buyer company's user outside an HTTP request
 * (for example from the order-events broker listener, where there is no caller JWT to
 * forward). Identity &amp; Access validates it locally with the same shared secret.
 */
@Configuration
public class ServiceTokenConfiguration {

    public static final String SERVICE_NAME = "fuelbridge-notification-service";

    @Bean
    public ServiceTokenIssuer serviceTokenIssuer(@Value("${authorization.jwt.secret}") String secret) {
        return new ServiceTokenIssuer(secret);
    }
}
