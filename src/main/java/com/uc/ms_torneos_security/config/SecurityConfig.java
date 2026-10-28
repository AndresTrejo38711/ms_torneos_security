package com.uc.ms_torneos_security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                // Sin sesión HTTP de Spring: cada petición se identifica con su token.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/logout", "/api/auth/me").authenticated()
                        // Etapa A de la HU-SE-08: el resto de la API sigue abierta.
                        // En la etapa B se exigirá token y permiso en todas las rutas.
                        .anyRequest().permitAll()
                )

                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(SecurityConfig::unauthorized)
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(SecurityConfig::unauthorized)
                );

        return http.build();
    }

    // 401 con el mismo formato JSON que usa GlobalExceptionHandler.
    private static void unauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        String message = request.getHeader(HttpHeaders.AUTHORIZATION) == null
                ? "Debes iniciar sesión: falta el token de acceso"
                : "El token no es válido o su sesión fue cerrada o expiró";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"errorCase\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}"
        );
    }
}
