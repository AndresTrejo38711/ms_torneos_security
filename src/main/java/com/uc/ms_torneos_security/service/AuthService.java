package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.auth.AuthResponseDTO;
import com.uc.ms_torneos_security.dto.auth.LoginRequestDTO;
import com.uc.ms_torneos_security.dto.auth.RegisterRequestDTO;
import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import com.uc.ms_torneos_security.entity.AuthProvider;
import com.uc.ms_torneos_security.entity.Session;
import com.uc.ms_torneos_security.entity.SessionStatus;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final UserService userService;

    private final SessionService sessionService;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    // Autorregistro con credenciales: UserService.create lo deja como usuario LOCAL
    // y guarda la contraseña cifrada con BCrypt, nunca en texto plano.
    public UserResponseDTO register(RegisterRequestDTO dto) {
        userRepository.findByEmail(dto.getEmail())
                .ifPresent(existing -> {
                    throw alreadyRegistered(existing);
                });
        return userService.create(dto);
    }

    // Login con correo y contraseña. Si el correo no existe, es de una cuenta OAuth o la
    // contraseña no coincide, responde lo mismo para no revelar qué correos están registrados.
    public AuthResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .filter(found -> found.getAuthProvider() == AuthProvider.LOCAL)
                .filter(found -> passwordEncoder.matches(dto.getPassword(), found.getPassword()))
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.UNAUTHORIZED,
                        "Credenciales incorrectas"
                ));

        if (!user.isEnabled()) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "La cuenta está deshabilitada"
            );
        }

        // Con 2FA la sesión queda pendiente y no se entrega token hasta validar el código (HU-SE-10).
        if (user.isTwoFactorEnabled()) {
            sessionService.create(user, SessionStatus.PENDING_2FA);
            return new AuthResponseDTO(null, null, null, true);
        }

        Session session = sessionService.create(user, SessionStatus.ACTIVE);
        return new AuthResponseDTO(
                jwtService.generateToken(session),
                "Bearer",
                session.getExpiresAt(),
                false
        );
    }

    // Cierra solo la sesión de este token; las demás sesiones del usuario siguen abiertas.
    public void logout(String tokenId) {
        sessionService.close(tokenId);
    }

    public UserResponseDTO me(Long userId) {
        return userService.findById(userId);
    }

    // Quien ya ingresa con Google, GitHub o Microsoft no debe registrarse a mano:
    // se le indica con qué proveedor iniciar sesión.
    private ApplicationException alreadyRegistered(User existing) {
        if (existing.getAuthProvider() != AuthProvider.LOCAL) {
            return new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Este correo ya está registrado con " + existing.getAuthProvider()
                            + ": inicia sesión con ese proveedor"
            );
        }
        return new ApplicationException(
                ErrorCase.ALREADY_EXISTS,
                "Ya existe una cuenta registrada con este correo"
        );
    }
}
