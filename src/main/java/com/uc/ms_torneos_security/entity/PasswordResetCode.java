package com.uc.ms_torneos_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Código para restablecer la contraseña (HU-SE-09). Solo sirve el último que pidió el usuario:
// pedir uno nuevo borra el anterior.
@Entity
@Table(name = "password_reset_codes")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetCode {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

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

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;
}
