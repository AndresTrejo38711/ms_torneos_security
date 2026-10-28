package com.uc.ms_torneos_security.dto.auth;

import com.uc.ms_torneos_security.dto.user.CreateUserDTO;

// Registro público (HU-SE-07): mismos campos y reglas que el alta hecha por un administrador
// (nombre, email y contraseña segura). Es una clase propia para que el registro pueda cambiar
// en el futuro sin afectar a /api/users.
public class RegisterRequestDTO extends CreateUserDTO {
}
