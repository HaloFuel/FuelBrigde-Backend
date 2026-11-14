package com.halofuel.fuelbridge.platform.iam.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.iam.application.commandservices.RevokedTokenCommandService;
import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.RevokedToken;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.RevokeTokenCommand;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.RevokedTokenRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RevokedTokenCommandServiceImpl implements RevokedTokenCommandService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final TokenService tokenService;

    public RevokedTokenCommandServiceImpl(RevokedTokenRepository revokedTokenRepository,
            TokenService tokenService) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.tokenService = tokenService;
    }

    @Override
    public Result<RevokedToken, ApplicationError> handle(RevokeTokenCommand command) {
        var token = command.token();
        if (token == null || token.isBlank() || !tokenService.validateToken(token)) {
            return Result.failure(ApplicationError.validationError("token", "A valid bearer token is required"));
        }
        var now = Instant.now();
        // Entries past their expiration are useless (the token is rejected anyway), so purge
        // them here to keep the table bounded.
        revokedTokenRepository.deleteByExpirationDateBefore(now);
        var revoked = new RevokedToken(token, tokenService.getExpirationFromToken(token), now);
        // Signing out twice with the same token is not an error.
        if (revokedTokenRepository.existsByTokenHash(revoked.getTokenHash())) {
            return Result.success(revoked);
        }
        return Result.success(revokedTokenRepository.save(revoked));
    }
}
