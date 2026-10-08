package com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources;

import java.util.List;

public record SignUpResource(String username, String password, List<String> roles, Long companyId, Long providerId) {
}
