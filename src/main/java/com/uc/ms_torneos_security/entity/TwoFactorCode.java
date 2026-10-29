package com.uc.ms_torneos_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Código de verificación del segundo factor (HU-SE-10). Pertenece a una sola sesión PENDING_2FA:
// el código sirve únicamente para terminar ese inicio de sesión.
@Entity
@Table(name = "two_factor_codes")
@Getter
@Setter
@NoArgsConstructor
public class TwoFactorCode {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    // Lo que recibe el cliente para decir qué inicio de sesión está completando.
    @Column(
            name = "challenge_id",
            nullable = false,
            unique = true,
            length = 36
    )
    private String challengeId;

    // El código se guarda cifrado con BCrypt, como las contraseñas.
    @Column(
            name = "code_hash",
            nullable = false,
            length = 60
    )
    private String codeHash;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(
            nullable = false
    )
    private int attempts = 0;

    // Con valor, el código ya se usó y no sirve otra vez.
    @Column(
            name = "used_at"
    )
    private LocalDateTime usedAt;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "session_id",
            nullable = false,
            unique = true
    )
    private Session session;
}
