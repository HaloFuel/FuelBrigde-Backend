package com.halofuel.fuelbridge.platform.iam.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.PasswordResetToken;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.User;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.ResetPasswordCommand;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.PasswordResetTokenRepository;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.UserRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @summary Pruebas unitarias de PasswordResetCommandServiceImpl.
 * @remarks Usa mocks de repositorios y del servicio de hashing para verificar la emisión del token
 * (solo se guarda el hash) y el restablecimiento con tokens válidos, expirados, usados e inexistentes.
 * @author HaloFuel
 */
class PasswordResetCommandServiceImplTest {

    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private HashingService hashingService;
    private PasswordResetCommandServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        hashingService = mock(HashingService.class);
        service = new PasswordResetCommandServiceImpl(userRepository, tokenRepository, hashingService);
        user = new User("ana@empresa.pe", "old-hash");
        user.setId(5L);
        when(userRepository.findByUsername("ana@empresa.pe")).thenReturn(Optional.of(user));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(hashingService.encode("NuevaClave!2026")).thenReturn("new-hash");
    }

    private PasswordResetToken storedToken(String raw, Instant expiresAt, boolean used) {
        var token = new PasswordResetToken(5L, raw, expiresAt);
        token.setUsed(used);
        when(tokenRepository.findByTokenHash(PasswordResetToken.hash(raw))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void requestingAResetStoresOnlyTheHashAndReturnsTheRawUuid() {
        var result = service.handle(new RequestPasswordResetCommand(" ana@empresa.pe "));

        assertThat(result.isSuccess()).isTrue();
        var pair = ((Result.Success<org.apache.commons.lang3.tuple.ImmutablePair<PasswordResetToken, String>, ?>) result).value();
        var raw = pair.getRight();
        assertThat(java.util.UUID.fromString(raw)).isNotNull();
        var captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(PasswordResetToken.hash(raw)).isNotEqualTo(raw);
        assertThat(captor.getValue().getUserId()).isEqualTo(5L);
        verify(tokenRepository).invalidateUnusedByUserId(5L);
    }

    @Test
    void requestingAResetForUnknownOrBlankUserFails() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThat(service.handle(new RequestPasswordResetCommand("nadie")).isFailure()).isTrue();
        assertThat(service.handle(new RequestPasswordResetCommand("  ")).isFailure()).isTrue();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void resettingWithAValidTokenChangesThePasswordAndConsumesTheToken() {
        var token = storedToken("good", Instant.now().plusSeconds(600), false);

        var result = service.handle(new ResetPasswordCommand("good", "NuevaClave!2026"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void resettingWithExpiredUsedOrUnknownTokenFailsWithoutChangingAnything() {
        storedToken("expired", Instant.now().minusSeconds(1), false);
        storedToken("used", Instant.now().plusSeconds(600), true);
        when(tokenRepository.findByTokenHash(PasswordResetToken.hash("unknown"))).thenReturn(Optional.empty());

        for (var raw : new String[] {"expired", "used", "unknown", "", null}) {
            assertThat(service.handle(new ResetPasswordCommand(raw, "NuevaClave!2026")).isFailure())
                    .as("token %s", raw).isTrue();
        }
        assertThat(user.getPassword()).isEqualTo("old-hash");
        verify(userRepository, never()).save(any());
    }

    @Test
    void resettingWithAWeakPasswordFailsAndKeepsTheTokenUsable() {
        var token = storedToken("good", Instant.now().plusSeconds(600), false);

        assertThat(service.handle(new ResetPasswordCommand("good", "short")).isFailure()).isTrue();
        assertThat(service.handle(new ResetPasswordCommand("good", "  ")).isFailure()).isTrue();
        assertThat(service.handle(new ResetPasswordCommand("good", null)).isFailure()).isTrue();

        assertThat(token.isUsed()).isFalse();
        verify(userRepository, never()).save(any());
    }
}
