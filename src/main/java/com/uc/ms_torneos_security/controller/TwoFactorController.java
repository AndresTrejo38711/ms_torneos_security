package com.uc.ms_torneos_security.controller;

import com.uc.ms_torneos_security.dto.auth.AuthResponseDTO;
import com.uc.ms_torneos_security.dto.auth.TwoFactorToggleRequestDTO;
import com.uc.ms_torneos_security.dto.auth.VerifyTwoFactorRequestDTO;
import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import com.uc.ms_torneos_security.service.AuthService;
import com.uc.ms_torneos_security.service.TwoFactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

// Verificación en dos pasos (HU-SE-10).
@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final AuthService authService;

    private final TwoFactorService twoFactorService;

    // Ruta pública: quien está completando el login todavía no tiene token.
    @PostMapping("/verify")
    public AuthResponseDTO verify(@Valid @RequestBody VerifyTwoFactorRequestDTO dto) {
        return authService.verifyTwoFactor(dto);
    }

    // Requiere token: cada usuario activa o desactiva el 2FA de su propia cuenta.
    @PostMapping("/enable")
    public UserResponseDTO enable(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TwoFactorToggleRequestDTO dto) {
        return twoFactorService.enable(Long.valueOf(jwt.getSubject()), dto.getPassword());
    }

    @PostMapping("/disable")
    public UserResponseDTO disable(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TwoFactorToggleRequestDTO dto) {
        return twoFactorService.disable(Long.valueOf(jwt.getSubject()), dto.getPassword());
    }
}
