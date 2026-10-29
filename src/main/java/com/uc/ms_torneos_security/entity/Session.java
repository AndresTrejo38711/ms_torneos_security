package com.uc.ms_torneos_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    // Identificador de la sesión que viaja dentro del JWT (claim "jti").
    // Se guarda el identificador y no el token completo: si alguien leyera la tabla,
    // no obtendría tokens que pueda usar.
    @Column(
            name = "token_id",
            nullable = false,
            unique = true,
            length = 36
    )
    private String tokenId;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 15
    )
    private SessionStatus status;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(
            name = "closed_at"
    )
    private LocalDateTime closedAt;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    // Solo las sesiones que pasaron por el 2FA tienen código. Se borra junto con la sesión.
    @OneToOne(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private TwoFactorCode twoFactorCode;
}
