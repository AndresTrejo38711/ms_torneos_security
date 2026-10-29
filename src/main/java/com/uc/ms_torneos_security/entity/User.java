package com.uc.ms_torneos_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                // Una cuenta de un proveedor (Google, GitHub, Microsoft) corresponde a un solo usuario.
                // Los usuarios LOCAL tienen provider_id NULL y MySQL no los compara entre sí.
                @UniqueConstraint(
                        name = "uk_user_provider",
                        columnNames = {"auth_provider", "provider_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            nullable = false,
            unique = true,
            length = 150
    )
    private String email;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "auth_provider",
            nullable = false,
            length = 20
    )
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(
            name = "provider_id",
            length = 100
    )
    private String providerId;

    @Column(
            nullable = false
    )
    private boolean enabled = true;

    @Column(
            name = "two_factor_enabled",
            nullable = false
    )
    private boolean twoFactorEnabled = false;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Profile profile;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<UserRole> userRoles = new ArrayList<>();

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Session> sessions = new ArrayList<>();

    // Códigos para restablecer la contraseña (HU-SE-09). Se borran junto con el usuario.
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<PasswordResetCode> passwordResetCodes = new ArrayList<>();
}
