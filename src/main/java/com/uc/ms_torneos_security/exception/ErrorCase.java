package com.uc.ms_torneos_security.exception;

import org.springframework.http.HttpStatus;

// Cada caso sabe su código HTTP: lo usan GlobalExceptionHandler y el login con OAuth,
// que responde fuera de los controladores.
public enum ErrorCase {
    NOT_FOUND(HttpStatus.NOT_FOUND),
    ALREADY_EXISTS(HttpStatus.CONFLICT),
    INVALID_OPERATION(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN);

    private final HttpStatus status;

    ErrorCase(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
