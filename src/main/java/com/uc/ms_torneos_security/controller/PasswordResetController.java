package com.uc.ms_torneos_security.controller;

import com.uc.ms_torneos_security.dto.auth.ForgotPasswordRequestDTO;
import com.uc.ms_torneos_security.dto.auth.MessageResponseDTO;
import com.uc.ms_torneos_security.dto.auth.ResetPasswordRequestDTO;
import com.uc.ms_torneos_security.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// Recuperación de contraseña (HU-SE-09). Rutas públicas: quien olvidó su contraseña no tiene token.
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // Paso 1: envía al correo un código para restablecer la contraseña.
    @PostMapping("/forgot")
    public MessageResponseDTO forgot(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        return passwordResetService.forgot(dto);
    }

    // Paso 2: con el código correcto guarda la contraseña nueva.
    @PostMapping("/reset")
    public MessageResponseDTO reset(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        return passwordResetService.reset(dto);
    }
}
