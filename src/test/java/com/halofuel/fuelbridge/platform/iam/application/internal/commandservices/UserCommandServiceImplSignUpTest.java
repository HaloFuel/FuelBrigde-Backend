package com.halofuel.fuelbridge.platform.iam.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.SignUpCommand;
import com.halofuel.fuelbridge.platform.iam.domain.model.entities.Role;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.RoleRepository;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @summary Pruebas unitarias del registro de usuarios en UserCommandServiceImpl.
 * @remarks Usa mocks de repositorios y hashing para verificar que los usernames con el prefijo reservado
 * de servicios se rechazan con un error de validación sin tocar la persistencia, y que un registro
 * normal sigue funcionando.
 * @author HaloFuel
 */
class UserCommandServiceImplSignUpTest {

    private UserRepository userRepository;
    private HashingService hashingService;
    private RoleRepository roleRepository;
    private UserCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        hashingService = mock(HashingService.class);
        roleRepository = mock(RoleRepository.class);
        service = new UserCommandServiceImpl(userRepository, hashingService, mock(TokenService.class), roleRepository);
    }

    private SignUpCommand signUp(String username) {
        return new SignUpCommand(username, "secret", List.of(), 101L, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fuelbridge-x", "fuelbridge-ordering-service", "FUELBRIDGE-x", "FuelBridge-x",
            "  fuelbridge-x", "\tFuelBridge-x "})
    void reservedUsernamesAreRejectedWithoutTouchingPersistence(String username) {
        var result = service.handle(signUp(username));

        assertThat(result.isFailure()).isTrue();
        var error = ((Result.Failure<User, ApplicationError>) result).error();
        assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(error.message()).contains("username");
        assertThat(error.details()).contains("reserved");
        verifyNoInteractions(userRepository, hashingService, roleRepository);
    }

    @Test
    void ordinaryUsernameStillRegisters() {
        var role = new Role(Role.getDefaultRole().getName());
        when(userRepository.existsByUsername("compras.costasur")).thenReturn(false);
        when(roleRepository.findByName(role.getName())).thenReturn(Optional.of(role));
        when(hashingService.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        var stored = new User("compras.costasur", "encoded", List.of(role), 101L, null);
        when(userRepository.findByUsername("compras.costasur")).thenReturn(Optional.of(stored));

        var result = service.handle(signUp("compras.costasur"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.toOptional().orElseThrow().getUsername()).isEqualTo("compras.costasur");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void usernameThatOnlyContainsThePrefixInTheMiddleStillRegisters() {
        var role = Role.getDefaultRole();
        when(userRepository.existsByUsername("my-fuelbridge-user")).thenReturn(false);
        when(roleRepository.findByName(role.getName())).thenReturn(Optional.of(role));
        when(hashingService.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByUsername("my-fuelbridge-user"))
                .thenReturn(Optional.of(new User("my-fuelbridge-user", "encoded", List.of(role), 101L, null)));

        assertThat(service.handle(signUp("my-fuelbridge-user")).isSuccess()).isTrue();
    }
}
