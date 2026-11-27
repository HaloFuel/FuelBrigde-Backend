package com.halofuel.fuelbridge.platform.iam.domain.model.aggregates;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @summary Pruebas unitarias del perfil editable del agregado User.
 * @remarks Verifica updateProfile(): normalización de espacios, teléfono opcional y los
 * rechazos con IllegalArgumentException, sin contexto de Spring ni mocks.
 * @author HaloFuel
 */
class UserTest {

    private User newUser() {
        return new User("compras.costasur", "hash");
    }

    @Test
    void newUserHasNoProfileData() {
        var user = newUser();

        assertThat(user.getFullName()).isNull();
        assertThat(user.getPhone()).isNull();
    }

    @Test
    void updateProfileStoresTrimmedValues() {
        var user = newUser();

        user.updateProfile("  Rosa Medina Paredes ", " +51987654321 ");

        assertThat(user.getFullName()).isEqualTo("Rosa Medina Paredes");
        assertThat(user.getPhone()).isEqualTo("+51987654321");
    }

    @Test
    void updateProfileWithBlankPhoneClearsIt() {
        var user = newUser();
        user.updateProfile("Rosa Medina", "+51987654321");

        user.updateProfile("Rosa Medina", "   ");

        assertThat(user.getPhone()).isNull();
    }

    @Test
    void updateProfileDoesNotTouchCredentialsOrUsername() {
        var user = newUser();

        user.updateProfile("Rosa Medina", null);

        assertThat(user.getUsername()).isEqualTo("compras.costasur");
        assertThat(user.getPassword()).isEqualTo("hash");
    }

    @Test
    void updateProfileRejectsBlankFullNameAndKeepsPreviousData() {
        var user = newUser();
        user.updateProfile("Rosa Medina", "+51987654321");

        assertThatThrownBy(() -> user.updateProfile("  ", "+51900000000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> user.updateProfile(null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(user.getFullName()).isEqualTo("Rosa Medina");
        assertThat(user.getPhone()).isEqualTo("+51987654321");
    }

    @Test
    void updateProfileRejectsTooLongValues() {
        var user = newUser();

        assertThatThrownBy(() -> user.updateProfile("a".repeat(User.MAX_FULL_NAME_LENGTH + 1), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> user.updateProfile("Rosa", "9".repeat(User.MAX_PHONE_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
