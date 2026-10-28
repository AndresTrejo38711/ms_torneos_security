package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.auth.RegisterRequestDTO;
import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import com.uc.ms_torneos_security.entity.AuthProvider;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final UserService userService;

    // Autorregistro con credenciales: UserService.create lo deja como usuario LOCAL
    // y guarda la contraseña cifrada con BCrypt, nunca en texto plano.
    public UserResponseDTO register(RegisterRequestDTO dto) {
        userRepository.findByEmail(dto.getEmail())
                .ifPresent(existing -> {
                    throw alreadyRegistered(existing);
                });
        return userService.create(dto);
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
