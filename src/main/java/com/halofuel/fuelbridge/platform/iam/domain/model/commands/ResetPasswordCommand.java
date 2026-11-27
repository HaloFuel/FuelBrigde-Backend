package com.halofuel.fuelbridge.platform.iam.domain.model.commands;

public record ResetPasswordCommand(String token, String newPassword) {
}
