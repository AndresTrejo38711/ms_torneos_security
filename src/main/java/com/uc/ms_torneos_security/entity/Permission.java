package com.uc.ms_torneos_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_permission_model_operation",
                        columnNames = {"model", "operation"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private ModuleType module;

    @Column(
            nullable = false,
            length = 50
    )
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 10
    )
    private Operation operation;

    // Endpoints que habilita este permiso. Son parte del permiso:
    // se crean, cambian y borran junto con él.
    @OneToMany(
            mappedBy = "permission",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<PermissionEndpoint> endpoints = new ArrayList<>();

    // Sin cascade: un permiso asignado a roles no se puede eliminar (ver PermissionService.delete).
    @OneToMany(
            mappedBy = "permission",
            fetch = FetchType.LAZY
    )
    private List<RolePermission> rolePermissions = new ArrayList<>();
}
