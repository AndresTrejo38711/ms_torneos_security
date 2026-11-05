package com.uc.ms_torneos_security.dto.permissionvalidation;

// Solo la decisión: no expone roles, permisos ni datos del usuario.
public record PermissionValidationResponseDTO(boolean allowed) {
}
