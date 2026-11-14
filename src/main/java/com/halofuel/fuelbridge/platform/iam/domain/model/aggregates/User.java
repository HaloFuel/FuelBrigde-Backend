package com.halofuel.fuelbridge.platform.iam.domain.model.aggregates;

import com.halofuel.fuelbridge.platform.iam.domain.model.entities.Role;
import com.halofuel.fuelbridge.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class User extends AbstractDomainAggregateRoot<User> {

    public static final int MAX_FULL_NAME_LENGTH = 120;
    public static final int MAX_PHONE_LENGTH = 20;

    @Setter private Long id;
    @Setter private String username;
    @Setter private String password;
    @Setter private Set<Role> roles;
    @Setter private Long companyId;
    @Setter private Long providerId;
    @Setter private String fullName;
    @Setter private String phone;

    public User() {
        this.roles = new HashSet<>();
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.roles = new HashSet<>();
    }

    public User(String username, String password, List<Role> roles, Long companyId, Long providerId) {
        this(username, password);
        addRoles(roles);
        this.companyId = companyId;
        this.providerId = providerId;
    }

    /**
     * Replaces the editable profile data. The full name is mandatory; the phone is optional
     * and a blank phone clears it.
     */
    public User updateProfile(String fullName, String phone) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        var trimmedName = fullName.trim();
        if (trimmedName.length() > MAX_FULL_NAME_LENGTH) {
            throw new IllegalArgumentException("Full name must not exceed " + MAX_FULL_NAME_LENGTH + " characters");
        }
        var trimmedPhone = phone == null || phone.isBlank() ? null : phone.trim();
        if (trimmedPhone != null && trimmedPhone.length() > MAX_PHONE_LENGTH) {
            throw new IllegalArgumentException("Phone must not exceed " + MAX_PHONE_LENGTH + " characters");
        }
        this.fullName = trimmedName;
        this.phone = trimmedPhone;
        return this;
    }

    /** Replaces the credentials with an already encoded password; never pass a raw password. */
    public User changePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("Encoded password is required");
        }
        this.password = encodedPassword;
        return this;
    }

    public User addRole(Role role) {
        this.roles.add(role);
        return this;
    }

    public User addRoles(List<Role> roles) {
        var validated = Role.validateRoleSet(roles);
        this.roles.addAll(validated);
        return this;
    }
}
