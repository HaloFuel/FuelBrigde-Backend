package com.halofuel.fuelbridge.platform.iam.domain.model.aggregates;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @summary Pruebas unitarias del agregado RevokedToken.
 * @remarks Verifica que solo se conserve el hash SHA-256 del token y las validaciones de entrada,
 * sin contexto de Spring ni mocks.
 * @author HaloFuel
 */
class RevokedTokenTest {

    @Test
    void storesSha256HashInsteadOfTheToken() {
        var revoked = new RevokedToken("abc", Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));

        // Well-known SHA-256 of "abc".
        assertThat(revoked.getTokenHash())
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(revoked.getExpirationDate()).isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
        assertThat(revoked.getRevokedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void hashIsDeterministicAndDiffersPerToken() {
        assertThat(RevokedToken.hash("token-a")).isEqualTo(RevokedToken.hash("token-a"));
        assertThat(RevokedToken.hash("token-a")).isNotEqualTo(RevokedToken.hash("token-b"));
    }

    @Test
    void rejectsBlankTokenOrMissingExpiration() {
        assertThatThrownBy(() -> new RevokedToken(" ", Instant.now(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RevokedToken("abc", null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
