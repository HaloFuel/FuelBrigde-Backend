package com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.iam.domain.model.commands.RevokeTokenCommand;

public final class RevokeTokenCommandFromAuthorizationHeaderAssembler {

    private static final String BEARER_PREFIX = "Bearer ";

    private RevokeTokenCommandFromAuthorizationHeaderAssembler() {
    }

    public static RevokeTokenCommand toCommandFromHeader(String authorizationHeader) {
        var token = authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)
                ? authorizationHeader.substring(BEARER_PREFIX.length()).trim()
                : null;
        return new RevokeTokenCommand(token);
    }
}
