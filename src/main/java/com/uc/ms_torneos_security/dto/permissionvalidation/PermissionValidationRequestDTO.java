package com.uc.ms_torneos_security.dto.permissionvalidation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Lo que otro microservicio pregunta: ¿el usuario del token puede invocar este método en esta URL?
// La URL es la ruta registrada en los permisos (ej.: /torneos/tournaments o /torneos/tournaments/15).
@Getter
@Setter
public class PermissionValidationRequestDTO {

    @NotBlank(
            message = "La url es obligatoria"
    )
    @Size(
            max = 255,
            message = "La url no puede superar 255 caracteres"
    )
    private String url;

    @NotBlank(
            message = "El método es obligatorio"
    )
    @Pattern(
            regexp = "GET|POST|PUT|PATCH|DELETE",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "El método HTTP no es válido"
    )
    private String method;
}
