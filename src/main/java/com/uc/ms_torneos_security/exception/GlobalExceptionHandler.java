package com.uc.ms_torneos_security.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Map<String, String>> handleApplicationException(
            ApplicationException exception) {

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", exception.getErrorCase().name());
        error.put("message", exception.getMessage());

        return ResponseEntity
                .status(exception.getErrorCase().getStatus())
                .body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException exception) {

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "HTTP_ERROR");
        error.put("message", exception.getReason());

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(
            HttpMessageNotReadableException exception) {

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "INVALID_BODY");
        error.put("message", "El cuerpo de la petición no es válido: revisa el formato JSON y los valores permitidos de cada campo");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // Un valor de la URL que no es del tipo esperado, por ejemplo /api/users/abc cuando el id es un número.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "INVALID_PARAMETER");
        error.put("message", "El valor \"" + exception.getValue() + "\" no es válido para " + exception.getName());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // El cuerpo no viene como JSON (por ejemplo, Content-Type: text/plain).
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception) {

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "UNSUPPORTED_MEDIA_TYPE");
        error.put("message", "El cuerpo de la petición debe enviarse como JSON (Content-Type: application/json)");

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(error);
    }

    // La ruta existe pero no con ese método, por ejemplo DELETE /api/auth/me/profile.
    // El encabezado Allow dice qué métodos sí acepta.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {

        Set<HttpMethod> supported = exception.getSupportedHttpMethods();

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "METHOD_NOT_ALLOWED");
        error.put("message", "La ruta no acepta el método " + exception.getMethod());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(supported == null ? new HttpMethod[0] : supported.toArray(HttpMethod[]::new))
                .body(error);
    }

    // Una URL que ningún controlador atiende. Pasa, por ejemplo, con un permiso registrado para
    // una ruta de otro microservicio: el permiso deja pasar la petición, pero aquí no existe.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFound(
            NoResourceFoundException exception) {

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "NOT_FOUND");
        error.put("message", "La ruta /" + exception.getResourcePath() + " no existe en este servicio");

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    // La base de datos rechazó el cambio por una regla de unicidad o de relación. Los services ya
    // validan estos casos; esto cubre, por ejemplo, dos registros con el mismo correo a la vez.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {

        log.warn("Cambio rechazado por la base de datos: {}", exception.getMostSpecificCause().getMessage());

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "DATA_CONFLICT");
        error.put("message", "El cambio choca con datos que ya existen o que dependen de este registro. Recarga e intenta de nuevo");

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    // Lo que no se esperaba: al cliente no se le muestran detalles, pero la causa queda en la consola.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), exception);

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "INTERNAL_ERROR");
        error.put("message", "Ocurrió un error interno en el servidor");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
