package com.halofuel.fuelbridge.platform.payment.infrastructure.security.tokens;

public interface TokenService {
    String generateToken(String username);
    String getUsernameFromToken(String token);
    boolean validateToken(String token);
}
