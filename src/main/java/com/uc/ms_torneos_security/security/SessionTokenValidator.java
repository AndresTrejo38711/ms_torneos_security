package com.uc.ms_torneos_security.security;

import com.uc.ms_torneos_security.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// Spring ya revisa la firma, el emisor y el vencimiento del JWT. Este validador agrega lo que
// pide la HU-SE-08: que la sesión del token siga ACTIVE en la base de datos. Así, después
// del logout el token deja de servir aunque todavía no haya vencido.
@Component
@RequiredArgsConstructor
public class SessionTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final SessionService sessionService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getId() != null && sessionService.isActive(jwt.getId())) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "La sesión fue cerrada, expiró o el usuario está deshabilitado",
                null
        ));
    }
}
