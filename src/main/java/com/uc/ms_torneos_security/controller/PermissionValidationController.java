package com.uc.ms_torneos_security.controller;

import com.uc.ms_torneos_security.dto.permissionvalidation.PermissionValidationRequestDTO;
import com.uc.ms_torneos_security.dto.permissionvalidation.PermissionValidationResponseDTO;
import com.uc.ms_torneos_security.entity.HttpMethodType;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

// Validación remota de permisos: otro microservicio reenvía el JWT que recibió del cliente y
// pregunta si ese usuario puede invocar una URL y un método. Responde 200 con la decisión;
// convertir "allowed: false" en 403 le toca al microservicio que consulta.
@RestController
@RequestMapping("/api/internal/security")
@RequiredArgsConstructor
public class PermissionValidationController {

    private final PermissionService permissionService;

    @PostMapping("/permissions-validation")
    public PermissionValidationResponseDTO validate(
            @Valid @RequestBody PermissionValidationRequestDTO dto,
            JwtAuthenticationToken authentication) {

        // El usuario sale del token ("sub"), nunca del cuerpo: así no se pueden consultar
        // los permisos de otro usuario.
        Long userId = Long.valueOf(authentication.getToken().getSubject());

        // El DTO ya validó que el método es uno de los cinco, en mayúsculas o minúsculas.
        boolean allowed;
        try {
            allowed = permissionService.hasPermission(
                    userId,
                    HttpMethodType.valueOf(dto.getMethod().toUpperCase(Locale.ROOT)),
                    dto.getUrl()
            );
        } catch (IllegalArgumentException exception) {
            // Un "%" que no forma un carácter (ej.: /torneos/%zz): la URL no se puede leer como ruta.
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "La url no es una ruta válida"
            );
        }

        return new PermissionValidationResponseDTO(allowed);
    }
}
