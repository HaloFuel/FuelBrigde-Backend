package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "revoked_tokens", indexes = @Index(name = "idx_revoked_tokens_expiration", columnList = "expirationDate"))
@Getter
@Setter
@NoArgsConstructor
public class RevokedTokenPersistenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expirationDate;

    @Column(nullable = false)
    private Instant revokedAt;
}
