package com.halofuel.fuelbridge.platform.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @summary Pruebas unitarias de ServiceIdentity.
 * @remarks Verifica que el filtro reconoce el subject de servicio de forma exacta y que el registro
 * reserva el prefijo también con mayúsculas y espacios, sin bloquear usernames normales.
 * @author HaloFuel
 */
class ServiceIdentityTest {

    @Test
    void serviceSubjectIsRecognisedOnlyByTheExactPrefix() {
        assertThat(ServiceIdentity.isServiceSubject("fuelbridge-ordering-service")).isTrue();
        assertThat(ServiceIdentity.isServiceSubject("FUELBRIDGE-ordering")).isFalse();
        assertThat(ServiceIdentity.isServiceSubject("compras.costasur")).isFalse();
        assertThat(ServiceIdentity.isServiceSubject(null)).isFalse();
    }

    @Test
    void reservedUsernameCoversCaseAndSurroundingWhitespace() {
        assertThat(ServiceIdentity.isReservedUsername("fuelbridge-x")).isTrue();
        assertThat(ServiceIdentity.isReservedUsername("FUELBRIDGE-x")).isTrue();
        assertThat(ServiceIdentity.isReservedUsername("FuelBridge-x")).isTrue();
        assertThat(ServiceIdentity.isReservedUsername("  fuelbridge-x")).isTrue();
        assertThat(ServiceIdentity.isReservedUsername("\tFuelBridge-x ")).isTrue();
    }

    @Test
    void ordinaryUsernamesAreNotReserved() {
        assertThat(ServiceIdentity.isReservedUsername("compras.costasur")).isFalse();
        assertThat(ServiceIdentity.isReservedUsername("fuelbridge")).isFalse();
        assertThat(ServiceIdentity.isReservedUsername("my-fuelbridge-user")).isFalse();
        assertThat(ServiceIdentity.isReservedUsername("")).isFalse();
        assertThat(ServiceIdentity.isReservedUsername(null)).isFalse();
    }
}
