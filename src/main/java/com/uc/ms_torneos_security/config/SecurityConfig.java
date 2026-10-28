package com.uc.ms_torneos_security.config;

import com.uc.ms_torneos_security.authorization.DynamicAuthorizationManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final DynamicAuthorizationManager dynamicAuthorizationManager;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                // Sin sesión HTTP de Spring: cada petición se identifica con su token.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // Públicas: quien se registra o inicia sesión todavía no tiene token.
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        // Página interna de errores de Spring: no debe convertir un error en 401.
                        .requestMatchers("/error").permitAll()
                        // Cualquier usuario con sesión puede ver quién es y cerrar su sesión.
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        // Todo lo demás: token válido Y permiso de alguno de sus roles.
                        .anyRequest().access(dynamicAuthorizationManager)
                )

                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(SecurityConfig::unauthorized)
                        .accessDeniedHandler(SecurityConfig::forbidden)
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(SecurityConfig::unauthorized)
                        .accessDeniedHandler(SecurityConfig::forbidden)
                );

        return http.build();
    }

    // 401: falta el token, no es válido o su sesión ya no está activa.
    private static void unauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        String message = request.getHeader(HttpHeaders.AUTHORIZATION) == null
                ? "Debes iniciar sesión: falta el token de acceso"
                : "El token no es válido o su sesión fue cerrada o expiró";

        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    // 403: el usuario sí inició sesión, pero ninguno de sus roles tiene el permiso.
    private static void forbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {

        writeError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                "Ninguno de tus roles tiene permiso para esta operación");
    }

    // Mismo formato JSON que usa GlobalExceptionHandler.
    private static void writeError(
            HttpServletResponse response,
            int status,
            String errorCase,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"errorCase\":\"" + errorCase + "\",\"message\":\"" + message + "\"}"
        );
    }
}
