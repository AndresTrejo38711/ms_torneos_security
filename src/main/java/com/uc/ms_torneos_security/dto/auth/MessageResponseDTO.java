package com.uc.ms_torneos_security.dto.auth;

import lombok.Value;

// Respuesta que solo informa el resultado, por ejemplo en la recuperación de contraseña.
@Value
public class MessageResponseDTO {

    String message;
}
