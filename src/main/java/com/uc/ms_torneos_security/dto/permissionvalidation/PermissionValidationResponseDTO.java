package com.uc.ms_torneos_security.dto.permissionvalidation;

// La decisión y, solo si se permite, quién hace la petición: su id y su correo (leído de la base, no
// del token). El microservicio que consulta lo necesita para lo que es "mío" (por ejemplo, los métodos
// de pago de un aficionado) y para escribirle. Cuando se niega, userId y email van en null: no expone
// roles, permisos ni datos del usuario.
public record PermissionValidationResponseDTO(boolean allowed, Long userId, String email) {

    public static PermissionValidationResponseDTO denied() {
        return new PermissionValidationResponseDTO(false, null, null);
    }
}
