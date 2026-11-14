package com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources;

import java.time.Instant;

public record PasswordResetTokenResource(String token, Instant expiresAt) {
}
