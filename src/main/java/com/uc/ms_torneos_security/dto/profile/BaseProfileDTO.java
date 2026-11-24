package com.uc.ms_torneos_security.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public abstract class BaseProfileDTO {

    @NotBlank(
            message = "El teléfono es obligatorio"
    )
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "El teléfono debe tener entre 7 y 15 dígitos, opcionalmente con + al inicio"
    )
    private String phone;

    // HU-SE-02: la foto llega como imagen pequeña en texto ("data:image/jpeg;base64,..."), con las mismas
    // reglas de ms_torneos_deportiva (common/validation/image.ts). No se aceptan SVG: pueden llevar
    // código adentro. Una URL también vale, para que un perfil que la tenía de antes se pueda guardar.
    @Size(
            max = 90_000,
            message = "La fotografía es demasiado grande: debe llegar reducida"
    )
    @Pattern(
            regexp = "^(data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/]+={0,2}|https?://\\S{1,493})$",
            message = "La fotografía debe ser una imagen JPG, PNG o WEBP"
    )
    private String photo;

    @Past(
            message = "La fecha de nacimiento debe ser una fecha pasada"
    )
    private LocalDate birthDate;
}
