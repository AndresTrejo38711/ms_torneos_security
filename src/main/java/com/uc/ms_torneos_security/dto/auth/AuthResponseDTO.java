package com.uc.ms_torneos_security.dto.auth;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class AuthResponseDTO {

    // null mientras falte validar el segundo factor.
    String accessToken;

    String tokenType;

    LocalDateTime expiresAt;

    // true cuando el usuario tiene 2FA: la sesión queda pendiente hasta enviar el código (HU-SE-10).
    boolean twoFactorRequired;
}
