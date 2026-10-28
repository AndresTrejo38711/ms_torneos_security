package com.uc.ms_torneos_security.dto.permission;

import com.uc.ms_torneos_security.entity.HttpMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EndpointRequestDTO {

    @NotBlank(
            message = "La URL es obligatoria"
    )
    @Size(
            max = 255,
            message = "La URL debe tener máximo 255 caracteres"
    )
    @Pattern(
            regexp = "^/\\S*$",
            message = "La URL debe empezar por / y no tener espacios (ej.: /api/users/{id})"
    )
    private String url;

    @NotNull(
            message = "El método HTTP es obligatorio (GET, POST, PUT, PATCH o DELETE)"
    )
    private HttpMethodType method;
}
