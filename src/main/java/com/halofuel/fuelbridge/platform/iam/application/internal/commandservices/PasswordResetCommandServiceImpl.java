package com.halofuel.fuelbridge.platform.iam.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.iam.application.commandservices.PasswordResetCommandService;
import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.PasswordResetToken;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.ResetPasswordCommand;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.PasswordResetTokenRepository;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetCommandServiceImpl implements PasswordResetCommandService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    // BCrypt only uses the first 72 bytes and rejects longer inputs.
    public static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final HashingService hashingService;

    public PasswordResetCommandServiceImpl(UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository, HashingService hashingService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.hashingService = hashingService;
    }

    @Override
    @Transactional
    public Result<ImmutablePair<PasswordResetToken, String>, ApplicationError> handle(
            RequestPasswordResetCommand command) {
        if (command.username() == null || command.username().isBlank()) {
            return Result.failure(ApplicationError.validationError("username", "email or username is required"));
        }
        var user = userRepository.findByUsername(command.username().trim());
        if (user.isEmpty()) {
            return Result.failure(ApplicationError.notFound("User", command.username().trim()));
        }
        var now = Instant.now();
        tokenRepository.deleteByExpiresAtBefore(now);
        // Only the latest token may be used: older outstanding ones are superseded.
        tokenRepository.invalidateUnusedByUserId(user.get().getId());
        var rawToken = UUID.randomUUID().toString();
        var saved = tokenRepository.save(PasswordResetToken.issue(user.get().getId(), rawToken, now));
        return Result.success(ImmutablePair.of(saved, rawToken));
    }

    @Override
    @Transactional
    public Result<Void, ApplicationError> handle(ResetPasswordCommand command) {
        var passwordError = validatePassword(command.newPassword());
        if (passwordError != null) {
            return Result.failure(passwordError);
        }
        if (command.token() == null || command.token().isBlank()) {
            return Result.failure(invalidToken());
        }
        var stored = tokenRepository.findByTokenHash(PasswordResetToken.hash(command.token().trim()));
        if (stored.isEmpty()) {
            return Result.failure(invalidToken());
        }
        var token = stored.get();
        try {
            token.markUsed(Instant.now());
        } catch (IllegalStateException ex) {
            // Unknown, expired and used tokens all answer the same, so the response does not
            // tell an attacker which tokens exist.
            return Result.failure(invalidToken());
        }
        var user = userRepository.findById(token.getUserId());
        if (user.isEmpty()) {
            return Result.failure(invalidToken());
        }
        user.get().changePassword(hashingService.encode(command.newPassword()));
        userRepository.save(user.get());
        tokenRepository.save(token);
        return Result.success(null);
    }

    private static ApplicationError validatePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            return ApplicationError.validationError("newPassword", "newPassword is required");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            return ApplicationError.validationError("newPassword",
                    "newPassword must have at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return ApplicationError.validationError("newPassword",
                    "newPassword must not exceed " + MAX_PASSWORD_BYTES + " bytes");
        }
        return null;
    }

    private static ApplicationError invalidToken() {
        return ApplicationError.validationError("token", "Invalid, expired or already used reset token");
    }
}
