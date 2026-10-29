package com.halofuel.fuelbridge.platform.iam.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.iam.application.commandservices.UserCommandService;
import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.SignInCommand;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.SignUpCommand;
import com.halofuel.fuelbridge.platform.iam.domain.model.entities.Role;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.RoleRepository;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final RoleRepository roleRepository;

    public UserCommandServiceImpl(UserRepository userRepository, HashingService hashingService,
            TokenService tokenService, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
    }

    @Override
    public Result<ImmutablePair<User, String>, ApplicationError> handle(SignInCommand command) {
        var user = userRepository.findByUsername(command.username());
        if (user.isEmpty()) {
            return Result.failure(ApplicationError.notFound("User", command.username()));
        }
        // Verify against the stored hash; hashing the input again would produce a new salt.
        if (!hashingService.matches(command.password(), user.get().getPassword())) {
            return Result.failure(ApplicationError.validationError("credentials", "Invalid username or password"));
        }
        // Issue a token only after credential verification succeeds.
        var token = tokenService.generateToken(user.get().getUsername());
        return Result.success(ImmutablePair.of(user.get(), token));
    }

    @Override
    public Result<User, ApplicationError> handle(SignUpCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            return Result.failure(ApplicationError.conflict("User", "Username already exists"));
        }
        var requestedRoles = command.roles().isEmpty()
                ? List.of(Role.getDefaultRole())
                : command.roles();
        // Resolve even the default role to a persisted entity so the user does not
        // reference a transient Role when its associations are saved.
        var roles = requestedRoles.stream()
                .map(role -> roleRepository.findByName(role.getName()))
                .toList();
        if (roles.stream().anyMatch(java.util.Optional::isEmpty)) {
            return Result.failure(ApplicationError.notFound("Role", "one or more role names"));
        }
        var resolvedRoles = roles.stream().map(java.util.Optional::get).toList();
        var user = new User(command.username(), hashingService.encode(command.password()),
                resolvedRoles, command.companyId(), command.providerId());
        userRepository.save(user);
        return userRepository.findByUsername(command.username())
                .<Result<User, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ApplicationError.unexpected("sign-up", "Created user could not be reloaded")));
    }
}
