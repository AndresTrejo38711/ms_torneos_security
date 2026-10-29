package com.uc.ms_torneos_security.security;

import com.uc.ms_torneos_security.dto.auth.AuthResponseDTO;
import com.uc.ms_torneos_security.entity.AuthProvider;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

// Último paso del login con Google, GitHub o Microsoft (HU-SE-08). Cuando se llega aquí, el
// proveedor ya validó la identidad; falta reconocer o crear el usuario interno y abrir su sesión,
// igual que en el login con correo y contraseña.
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final AuthService authService;

    private final JsonMapper jsonMapper;

    // Página del front que recibe el resultado en el fragmento (#) de la URL.
    // Vacía: se responde JSON, igual que /api/auth/login.
    @Value("${security.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        endHandshake(request);

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        AuthProvider provider = AuthProvider.valueOf(
                token.getAuthorizedClientRegistrationId().toUpperCase()
        );
        OAuth2User providerUser = token.getPrincipal();
        String email = email(provider, providerUser);

        try {
            AuthResponseDTO session = authService.loginWithOAuth(
                    provider,
                    providerUser.getName(),
                    email,
                    firstNonBlank(providerUser.getAttribute("name"), providerUser.getAttribute("login"), email)
            );
            writeSession(response, session);
        } catch (ApplicationException exception) {
            writeError(response, exception.getErrorCase(), exception.getMessage());
        }
    }

    // El usuario canceló en la página del proveedor o la autorización no se pudo completar.
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        endHandshake(request);
        // Al usuario no se le muestran detalles internos; la causa queda en la consola del backend
        // (por ejemplo, un client-secret equivocado aparece como "invalid_client").
        log.warn("Login con proveedor fallido: {}", exception.getMessage());
        writeError(response, ErrorCase.UNAUTHORIZED,
                "No se completó el inicio de sesión con el proveedor: se canceló o no fue autorizado");
    }

    // Cada proveedor entrega el correo a su manera.
    private static String email(AuthProvider provider, OAuth2User user) {
        return switch (provider) {
            case GOOGLE -> Boolean.TRUE.equals(user.getAttribute("email_verified"))
                    ? user.getAttribute("email")
                    : null;
            // Algunas cuentas de Microsoft no traen "email": su usuario de ingreso es el correo.
            case MICROSOFT -> firstNonBlank(user.getAttribute("email"), user.getAttribute("preferred_username"));
            // GitHub: GitHubOAuth2UserService ya dejó aquí el correo principal y verificado.
            default -> user.getAttribute("email");
        };
    }

    private static String firstNonBlank(String... values) {
        return Stream.of(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    // La sesión HTTP solo sirvió para el ida y vuelta con el proveedor (guardaba el "state" y
    // los tokens del proveedor). Desde aquí el usuario se identifica con el JWT de la plataforma.
    private static void endHandshake(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private void writeSession(HttpServletResponse response, AuthResponseDTO session) throws IOException {
        if (redirectUri.isBlank()) {
            writeJson(response, HttpServletResponse.SC_OK, session);
            return;
        }
        response.sendRedirect(redirectUri
                + "#accessToken=" + encode(session.getAccessToken())
                + "&tokenType=" + encode(session.getTokenType())
                + "&expiresAt=" + encode(session.getExpiresAt().toString()));
    }

    // Mismo formato de error que el resto de la API.
    private void writeError(HttpServletResponse response, ErrorCase errorCase, String message) throws IOException {
        if (redirectUri.isBlank()) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("errorCase", errorCase.name());
            error.put("message", message);
            writeJson(response, errorCase.getStatus().value(), error);
            return;
        }
        response.sendRedirect(redirectUri
                + "#errorCase=" + encode(errorCase.name())
                + "&message=" + encode(message));
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        jsonMapper.writeValue(response.getWriter(), body);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
