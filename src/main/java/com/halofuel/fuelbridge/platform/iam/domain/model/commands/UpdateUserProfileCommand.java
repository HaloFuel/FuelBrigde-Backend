package com.halofuel.fuelbridge.platform.iam.domain.model.commands;

public record UpdateUserProfileCommand(Long userId, String fullName, String phone) {
}
