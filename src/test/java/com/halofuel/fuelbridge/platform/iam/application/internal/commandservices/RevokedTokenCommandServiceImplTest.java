package com.halofuel.fuelbridge.platform.iam.application.internal.commandservices;

import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.halofuel.fuelbridge.platform.iam.domain.model.aggregates.RevokedToken;
import com.halofuel.fuelbridge.platform.iam.domain.model.commands.RevokeTokenCommand;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.RevokedTokenRepository;
import com.halofuel.fuelbridge.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @summary Pruebas unitarias de RevokedTokenCommandServiceImpl.
 * @remarks Usa mocks del repositorio y del servicio de tokens para verificar que se guarda solo el hash,
 * que revocar dos veces es idempotente y que un token inválido se rechaza sin persistir.
 * @author HaloFuel
 */
class RevokedTokenCommandServiceImplTest {

    private static final Instant EXPIRATION = Instant.parse("2030-01-01T00:00:00Z");

    private RevokedTokenRepository repository;
    private TokenService tokenService;
    private RevokedTokenCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(RevokedTokenRepository.class);
        tokenService = mock(TokenService.class);
        service = new RevokedTokenCommandServiceImpl(repository, tokenService);
        when(repository.save(any(RevokedToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void revokingAValidTokenStoresItsHashAndExpiration() {
        when(tokenService.validateToken("jwt")).thenReturn(true);
        when(tokenService.getExpirationFromToken("jwt")).thenReturn(EXPIRATION);

        var result = service.handle(new RevokeTokenCommand("jwt"));

        assertThat(result.isSuccess()).isTrue();
        var captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(RevokedToken.hash("jwt")).isNotEqualTo("jwt");
        assertThat(captor.getValue().getExpirationDate()).isEqualTo(EXPIRATION);
        assertThat(captor.getValue().getRevokedAt()).isNotNull();
    }

    @Test
    void revokingAnAlreadyRevokedTokenSucceedsWithoutSavingAgain() {
        when(tokenService.validateToken("jwt")).thenReturn(true);
        when(tokenService.getExpirationFromToken("jwt")).thenReturn(EXPIRATION);
        when(repository.existsByTokenHash(RevokedToken.hash("jwt"))).thenReturn(true);

        var result = service.handle(new RevokeTokenCommand("jwt"));

        assertThat(result.isSuccess()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void revokingAnInvalidOrMissingTokenFailsWithValidationErrorAndSavesNothing() {
        when(tokenService.validateToken("bad")).thenReturn(false);

        var invalid = service.handle(new RevokeTokenCommand("bad"));
        var missing = service.handle(new RevokeTokenCommand(null));

        assertThat(invalid).isInstanceOfSatisfying(Result.Failure.class,
                f -> assertThat(f.error().toString()).contains("VALIDATION_ERROR"));
        assertThat(missing.isFailure()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void revokingPurgesEntriesThatAlreadyExpired() {
        when(tokenService.validateToken("jwt")).thenReturn(true);
        when(tokenService.getExpirationFromToken("jwt")).thenReturn(EXPIRATION);

        service.handle(new RevokeTokenCommand("jwt"));

        verify(repository).deleteByExpirationDateBefore(any(Instant.class));
    }
}
