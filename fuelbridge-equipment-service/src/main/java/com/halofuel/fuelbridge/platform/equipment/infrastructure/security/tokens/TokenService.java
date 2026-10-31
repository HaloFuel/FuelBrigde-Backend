package com.halofuel.fuelbridge.platform.equipment.infrastructure.security.tokens;

public interface TokenService {
    String generateToken(String username);
    String getUsernameFromToken(String token);
    boolean validateToken(String token);
}
