package com.uc.ms_torneos_security.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyTwoFactorRequestDTO {

    // El challengeId que devolvió el login: dice qué inicio de sesión se está completando.
    @NotBlank(
            message = "El challengeId es obligatorio"
    )
    private String challengeId;

    @NotBlank(
            message = "El código es obligatorio"
    )
    @Pattern(
            regexp = "^\\d{6}$",
            message = "El código debe tener 6 dígitos"
    )
    private String code;
}
