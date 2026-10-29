package com.uc.ms_torneos_security.config;

import com.uc.ms_torneos_security.authorization.DynamicAuthorizationManager;
import com.uc.ms_torneos_security.security.GitHubOAuth2UserService;
import com.uc.ms_torneos_security.security.OAuth2LoginHandler;
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
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final DynamicAuthorizationManager dynamicAuthorizationManager;

    private final GitHubOAuth2UserService gitHubOAuth2UserService;

    private final OAuth2LoginHandler oAuth2LoginHandler;

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
                        // Pública: el segundo paso del login con 2FA, cuando aún no hay token.
                        .requestMatchers(HttpMethod.POST, "/api/auth/2fa/verify").permitAll()
                        // Públicas: ida y vuelta con Google, GitHub o Microsoft.
                        .requestMatchers("/oauth2/authorization/*", "/login/oauth2/code/*").permitAll()
                        // Página interna de errores de Spring: no debe convertir un error en 401.
                        .requestMatchers("/error").permitAll()
                        // Cualquier usuario con sesión puede ver quién es, cerrar su sesión
                        // y activar o desactivar el 2FA de su propia cuenta.
                        .requestMatchers("/api/auth/me", "/api/auth/logout",
                                "/api/auth/2fa/enable", "/api/auth/2fa/disable").authenticated()
                        // Todo lo demás: token válido Y permiso de alguno de sus roles.
                        .anyRequest().access(dynamicAuthorizationManager)
                )

                // Login con un proveedor: /oauth2/authorization/{google|github|microsoft} lleva al
                // proveedor, que devuelve al usuario a /login/oauth2/code/{proveedor}. Spring valida
                // esa respuesta y oAuth2LoginHandler abre la sesión interna.
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(gitHubOAuth2UserService))
                        // Los tokens del proveedor solo sirven para leer los datos del usuario:
                        // quedan en la sesión HTTP del ida y vuelta, que el handler descarta.
                        .authorizedClientRepository(new HttpSessionOAuth2AuthorizedClientRepository())
                        .successHandler(oAuth2LoginHandler)
                        .failureHandler(oAuth2LoginHandler)
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
