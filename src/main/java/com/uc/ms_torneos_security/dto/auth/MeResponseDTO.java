package com.uc.ms_torneos_security.dto.auth;

import com.uc.ms_torneos_security.dto.permission.PermissionSummaryDTO;
import com.uc.ms_torneos_security.entity.AuthProvider;
import lombok.Value;

import java.util.List;

// Respuesta de /api/auth/me: los mismos datos de UserResponseDTO más los roles y permisos
// del usuario, para que el front arme el menú. Es una clase aparte para no calcular permisos
// en las demás rutas que devuelven usuarios.
@Value
public class MeResponseDTO {

    Long id;

    String name;

    String email;

    AuthProvider authProvider;

    boolean enabled;

    boolean twoFactorEnabled;

    // Nombres de los roles asignados; vacía si todavía no tiene ninguno.
    List<String> roles;

    // Permisos efectivos: unión sin repetidos de los permisos de todos sus roles.
    List<PermissionSummaryDTO> permissions;
}
