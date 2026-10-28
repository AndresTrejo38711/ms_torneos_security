package com.uc.ms_torneos_security.authorization;

import com.uc.ms_torneos_security.entity.HttpMethodType;
import com.uc.ms_torneos_security.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

// Decide, en cada petición protegida, si el usuario del token puede usar ese método y esa ruta.
// Los permisos se consultan en la BD cada vez: un cambio en la matriz aplica de inmediato,
// sin que el usuario tenga que volver a iniciar sesión.
@Component
@RequiredArgsConstructor
public class DynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final PermissionService permissionService;

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {

        // Sin un token válido no hay a quién revisarle permisos: Spring responde 401.
        if (!(authenticationSupplier.get() instanceof JwtAuthenticationToken authentication)) {
            return new AuthorizationDecision(false);
        }

        HttpServletRequest request = context.getRequest();

        HttpMethodType method;
        try {
            method = HttpMethodType.valueOf(request.getMethod());
        } catch (IllegalArgumentException exception) {
            // Métodos que ningún permiso puede tener (HEAD, OPTIONS...).
            return new AuthorizationDecision(false);
        }

        // El "sub" del token es el id del usuario.
        Long userId = Long.valueOf(authentication.getName());

        return new AuthorizationDecision(
                permissionService.hasPermission(userId, method, request.getRequestURI())
        );
    }
}
