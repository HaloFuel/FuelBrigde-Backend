package com.halofuel.fuelbridge.platform.shared.infrastructure.configuration;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Builds the CORS policy consumed by the Spring Security filter chain.
 * Configure app.cors.allowed-origins as a comma-separated list of origins or
 * origin patterns, including the scheme and any non-default port.
 */
@Configuration
public class WebCorsConfiguration {
    private final List<String> allowedOrigins;

    public WebCorsConfiguration(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use patterns (not exact origins) so wildcard values like "https://*.vercel.app"
        // work together with allowCredentials(true). Exact origins still match as plain patterns.
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Permit headers such as Authorization and Content-Type during preflight
        // so browser clients can send bearer tokens and JSON request bodies.
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // This policy applies to public registration and protected API routes;
        // endpoint access is still determined by Spring Security authorization.
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
