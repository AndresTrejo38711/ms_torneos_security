package com.uc.ms_torneos_security.dto.permission;

import com.uc.ms_torneos_security.entity.HttpMethodType;
import com.uc.ms_torneos_security.entity.ModuleType;
import com.uc.ms_torneos_security.entity.Operation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BasePermissionDTO {

    @NotNull(
            message = "El módulo es obligatorio (ADMINISTRACION, GESTION_DEPORTIVA, COMERCIAL, INFRAESTRUCTURA o REPORTES)"
    )
    private ModuleType module;

    @NotBlank(
            message = "El modelo o recurso es obligatorio"
    )
    @Size(
            max = 50,
            message = "El modelo debe tener máximo 50 caracteres"
    )
    @Pattern(
            regexp = "^[a-z][a-z0-9-]*$",
            message = "El modelo debe estar en minúsculas, sin espacios (ej.: users, user-roles)"
    )
    private String model;

    @NotNull(
            message = "La operación es obligatoria (VIEW, LIST, CREATE, UPDATE o DELETE)"
    )
    private Operation operation;

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
