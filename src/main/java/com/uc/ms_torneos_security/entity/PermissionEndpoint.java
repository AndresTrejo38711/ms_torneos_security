package com.uc.ms_torneos_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un endpoint (URL + método) que habilita un permiso. Un permiso puede tener varios:
// "usuarios · Actualizar" cubre PUT /api/users/{id}, PATCH .../disable y PATCH .../enable.
@Entity
@Table(
        name = "permission_endpoints",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_permission_endpoint_url_method",
                        columnNames = {"url", "method"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PermissionEndpoint {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    // Patrón de la URL: las partes variables van entre llaves, por ejemplo /api/users/{id}.
    @Column(
            nullable = false,
            length = 255
    )
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 10
    )
    private HttpMethodType method;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "permission_id",
            nullable = false
    )
    private Permission permission;
}
