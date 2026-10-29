package com.uc.ms_torneos_security.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Activar o desactivar el 2FA pide la contraseña actual: con solo un token robado no se puede cambiar.
@Getter
@Setter
public class TwoFactorToggleRequestDTO {

    @NotBlank(
            message = "La contraseña es obligatoria"
    )
    private String password;
}
