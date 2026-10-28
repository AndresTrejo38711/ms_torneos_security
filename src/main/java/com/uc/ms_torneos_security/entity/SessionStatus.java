package com.uc.ms_torneos_security.entity;

public enum SessionStatus {
    // Contraseña correcta, pero falta validar el segundo factor (HU-SE-10): todavía no da acceso.
    PENDING_2FA,
    ACTIVE,
    CLOSED
}
