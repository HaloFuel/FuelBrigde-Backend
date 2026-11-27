package com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources;

public record ResetPasswordResource(String token, String newPassword) {
}
