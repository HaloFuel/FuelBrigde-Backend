package com.halofuel.fuelbridge.platform.notification.infrastructure.security.tokens.jwt;

import com.halofuel.fuelbridge.platform.notification.infrastructure.security.tokens.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface BearerTokenService extends TokenService {
    String getBearerTokenFrom(HttpServletRequest request);
    String generateToken(Authentication authentication);
}
