package com.uc.ms_torneos_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            length = 20
    )
    private String phone;

    // La foto que se sube en "Mi perfil" (HU-SE-02), ya reducida por el front, guardada como texto
    // "data:image/jpeg;base64,...". Las que se registraron antes como URL siguen sirviendo.
    @Column(
            columnDefinition = "MEDIUMTEXT"
    )
    private String photo;

    @Column(
            name = "birth_date"
    )
    private LocalDate birthDate;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;
}
