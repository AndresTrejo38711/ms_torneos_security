package com.uc.ms_torneos_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 50
    )
    private String name;

    @Column(
            nullable = false,
            length = 255
    )
    private String description;

    // Sin cascade: eliminar un rol nunca debe borrar usuarios ni asignaciones en cadena.
    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY
    )
    private List<UserRole> userRoles = new ArrayList<>();

    // Con cascade: la matriz de permisos es configuración del propio rol. Al eliminar el rol
    // se borran sus asignaciones, pero los permisos siguen existiendo en el sistema.
    @OneToMany(
            mappedBy = "role",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<RolePermission> rolePermissions = new ArrayList<>();
}
