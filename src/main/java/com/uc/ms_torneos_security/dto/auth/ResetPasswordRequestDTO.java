package com.uc.ms_torneos_security.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDTO {

    @NotBlank(
            message = "El email es obligatorio"
    )
    @Email(
            message = "El email no tiene un formato válido"
    )
    private String email;

    // El código que llegó al correo después de /api/auth/password/forgot.
    @NotBlank(
            message = "El código es obligatorio"
    )
    @Pattern(
            regexp = "^\\d{6}$",
            message = "El código debe tener 6 dígitos"
    )
    private String code;

    // Mismas reglas de seguridad que la contraseña del registro (CreateUserDTO).
    @NotBlank(
            message = "La contraseña nueva es obligatoria"
    )
    @Size(
            min = 8,
            max = 16,
            message = "La contraseña debe tener entre 8 y 16 caracteres"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).*$",
            message = "La contraseña debe tener al menos una minúscula, una mayúscula, un número y un carácter especial"
    )
    private String newPassword;
}
