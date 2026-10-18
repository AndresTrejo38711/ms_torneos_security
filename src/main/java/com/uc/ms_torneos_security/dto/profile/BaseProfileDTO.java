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

    @Size(
            max = 500,
            message = "La URL de la fotografía debe tener máximo 500 caracteres"
    )
    @Pattern(
            regexp = "^https?://\\S+$",
            message = "La fotografía debe ser una URL válida que empiece por http:// o https://"
    )
    private String photo;

    @Past(
            message = "La fecha de nacimiento debe ser una fecha pasada"
    )
    private LocalDate birthDate;
}
