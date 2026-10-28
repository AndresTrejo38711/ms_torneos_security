package com.uc.ms_torneos_security.dto.permission;

import com.uc.ms_torneos_security.entity.ModuleType;
import com.uc.ms_torneos_security.entity.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
            message = "El modelo debe estar en minúsculas, sin espacios (ej.: usuarios, asignacion-roles)"
    )
    private String model;

    @NotNull(
            message = "La operación es obligatoria (VIEW, LIST, CREATE, UPDATE o DELETE)"
    )
    private Operation operation;

    // @Valid revisa también las reglas de cada endpoint (URL y método).
    @NotEmpty(
            message = "El permiso debe tener al menos un endpoint (URL + método)"
    )
    private List<
            @Valid
            @NotNull(message = "El endpoint no puede ser nulo")
            EndpointRequestDTO> endpoints;
}
