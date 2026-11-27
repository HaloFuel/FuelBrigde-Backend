package com.halofuel.fuelbridge.platform.iam.infrastructure.tokens.jwt;

import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.tokens.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

/**
 * BearerTokenService
 *
 * Extension of TokenService that adds Bearer token support for HTTP requests (TS-01).
 * Defines the contract for extracting Bearer tokens from incoming HTTP requests
 * and generating JWT tokens from Spring Security Authentication objects.
 */

public interface BearerTokenService extends TokenService {
    String getBearerTokenFrom(HttpServletRequest request);
    String generateToken(Authentication authentication);
}
