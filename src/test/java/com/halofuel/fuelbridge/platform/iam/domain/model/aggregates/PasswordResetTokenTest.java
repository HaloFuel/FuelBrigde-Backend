package com.halofuel.fuelbridge.platform.iam.domain.model.aggregates;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @summary Pruebas unitarias del agregado PasswordResetToken.
 * @remarks Verifica el hash del token, la vigencia de una hora y los estados válido, expirado y usado,
 * sin contexto de Spring ni mocks.
 * @author HaloFuel
 */
class PasswordResetTokenTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    private PasswordResetToken issued() {
        return PasswordResetToken.issue(7L, "raw-token", NOW);
    }

    @Test
    void issuedTokenIsValidForOneHourAndStoresOnlyTheHash() {
        var token = issued();

        assertThat(token.getUserId()).isEqualTo(7L);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getTokenHash()).isEqualTo(PasswordResetToken.hash("raw-token")).isNotEqualTo("raw-token");
        assertThat(token.getTokenHash()).hasSize(64);
    }

    @Test
    void validTokenIsUsableUntilItsExpiration() {
        var token = issued();

        assertThat(token.isUsable(NOW)).isTrue();
        assertThat(token.isUsable(NOW.plus(Duration.ofMinutes(59)))).isTrue();
    }

    @Test
    void tokenIsExpiredAtAndAfterItsExpirationInstant() {
        var token = issued();

        assertThat(token.isExpired(NOW.plus(Duration.ofHours(1)))).isTrue();
        assertThat(token.isUsable(NOW.plus(Duration.ofHours(2)))).isFalse();
    }

    @Test
    void markUsedConsumesAValidToken() {
        var token = issued();

        token.markUsed(NOW.plusSeconds(10));

        assertThat(token.isUsed()).isTrue();
        assertThat(token.isUsable(NOW.plusSeconds(11))).isFalse();
    }

    @Test
    void markUsedRejectsAnExpiredToken() {
        var token = issued();

        assertThatThrownBy(() -> token.markUsed(NOW.plus(Duration.ofHours(1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void markUsedRejectsAnAlreadyUsedToken() {
        var token = issued();
        token.markUsed(NOW);

        assertThatThrownBy(() -> token.markUsed(NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void rejectsMissingData() {
        assertThatThrownBy(() -> new PasswordResetToken(null, "t", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PasswordResetToken(1L, " ", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PasswordResetToken(1L, "t", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
