package com.uc.ms_torneos_security.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDTO
        extends BaseUserDTO {

    // Opcional: si no viene, se conserva la actual. Si viene, mismas reglas que CreateUserDTO.
    @Size(
            min = 8,
            max = 16,
            message = "La contraseña debe tener entre 8 y 16 caracteres"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).*$",
            message = "La contraseña debe tener al menos una minúscula, una mayúscula, un número y un carácter especial"
    )
    private String password;
}
