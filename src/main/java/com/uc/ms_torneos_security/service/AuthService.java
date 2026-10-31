package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.auth.AuthResponseDTO;
import com.uc.ms_torneos_security.dto.auth.LoginRequestDTO;
import com.uc.ms_torneos_security.dto.auth.MeResponseDTO;
import com.uc.ms_torneos_security.dto.auth.RegisterRequestDTO;
import com.uc.ms_torneos_security.dto.auth.VerifyTwoFactorRequestDTO;
import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import com.uc.ms_torneos_security.entity.AuthProvider;
import com.uc.ms_torneos_security.entity.Permission;
import com.uc.ms_torneos_security.entity.Session;
import com.uc.ms_torneos_security.entity.SessionStatus;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.mapper.PermissionMapper;
import com.uc.ms_torneos_security.mapper.UserMapper;
import com.uc.ms_torneos_security.repository.PermissionRepository;
import com.uc.ms_torneos_security.repository.UserRepository;
import com.uc.ms_torneos_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final UserService userService;

    private final SessionService sessionService;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    private final TwoFactorService twoFactorService;

    private final UserRoleRepository userRoleRepository;

    private final PermissionRepository permissionRepository;

    private final UserMapper userMapper;

    private final PermissionMapper permissionMapper;

    // Autorregistro con credenciales: UserService.create lo deja como usuario LOCAL
    // y guarda la contraseña cifrada con BCrypt, nunca en texto plano.
    public UserResponseDTO register(RegisterRequestDTO dto) {
        userRepository.findByEmail(dto.getEmail())
                .ifPresent(existing -> {
                    throw alreadyRegistered(existing, "Ya existe una cuenta registrada con este correo");
                });
        return userService.create(dto);
    }

    // Login con correo y contraseña. Si el correo no existe, es de una cuenta OAuth o la
    // contraseña no coincide, responde lo mismo para no revelar qué correos están registrados.
    // Transaccional: si el código de 2FA no se puede enviar, no queda una sesión pendiente huérfana.
    @Transactional
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
            Session session = sessionService.create(user, SessionStatus.PENDING_2FA);
            String challengeId = twoFactorService.sendCode(session);
            return new AuthResponseDTO(null, null, null, true, challengeId);
        }

        return activeSession(user);
    }

    // Segundo paso del login con 2FA: con el código correcto la sesión pendiente pasa a ACTIVE.
    // noRollbackFor: un código incorrecto también se guarda (cuenta como intento fallido).
    @Transactional(noRollbackFor = ApplicationException.class)
    public AuthResponseDTO verifyTwoFactor(VerifyTwoFactorRequestDTO dto) {
        Session session = twoFactorService.checkCode(dto.getChallengeId(), dto.getCode());
        sessionService.activate(session);
        return authResponse(session);
    }

    // Login con Google, GitHub o Microsoft: el proveedor ya validó la identidad.
    // El usuario se reconoce por su id en el proveedor; en su primer ingreso se crea.
    @Transactional
    public AuthResponseDTO loginWithOAuth(
            AuthProvider provider,
            String providerId,
            String email,
            String name) {

        User user = userRepository.findByAuthProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createOAuthUser(provider, providerId, email, name));

        if (!user.isEnabled()) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "La cuenta está deshabilitada"
            );
        }

        // Sin 2FA: en las cuentas OAuth la seguridad del acceso la pone el proveedor.
        return activeSession(user);
    }

    private User createOAuthUser(
            AuthProvider provider,
            String providerId,
            String email,
            String name) {

        if (email == null || email.isBlank()) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    provider + " no compartió un correo verificado: no se puede crear la cuenta"
            );
        }

        // Un correo ya usado con contraseña o con otro proveedor no se reutiliza: se rechaza el ingreso.
        userRepository.findByEmail(email)
                .ifPresent(existing -> {
                    throw alreadyRegistered(existing,
                            "Este correo ya está registrado con contraseña: inicia sesión con tu correo y contraseña");
                });

        // Sin contraseña: quien entra con un proveedor no tiene contraseña en la plataforma.
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setAuthProvider(provider);
        user.setProviderId(providerId);
        return userRepository.save(user);
    }

    private AuthResponseDTO activeSession(User user) {
        return authResponse(sessionService.create(user, SessionStatus.ACTIVE));
    }

    private AuthResponseDTO authResponse(Session session) {
        return new AuthResponseDTO(
                jwtService.generateToken(session),
                "Bearer",
                session.getExpiresAt(),
                false,
                null
        );
    }

    // Cierra solo la sesión de este token; las demás sesiones del usuario siguen abiertas.
    public void logout(String tokenId) {
        sessionService.close(tokenId);
    }

    // Usuario de la sesión con sus roles y permisos efectivos, para que el front muestre solo
    // las opciones que puede usar. Ocultar opciones es solo visual: el backend igual valida cada petición.
    @Transactional(readOnly = true)
    public MeResponseDTO me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Usuario no encontrado con id: " + userId
                ));
        List<String> roles = userRoleRepository.findAllByUserId(userId).stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();
        List<Permission> permissions = permissionRepository.findEffectiveByUserId(userId);
        return userMapper.toMeResponseDTO(user, roles, permissionMapper.toSummaryDTOList(permissions));
    }

    // Si el correo es de una cuenta de Google, GitHub o Microsoft, se indica con qué proveedor
    // iniciar sesión; si es de una cuenta con contraseña, se usa el mensaje recibido.
    private ApplicationException alreadyRegistered(User existing, String localMessage) {
        if (existing.getAuthProvider() != AuthProvider.LOCAL) {
            return new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Este correo ya está registrado con " + existing.getAuthProvider()
                            + ": inicia sesión con ese proveedor"
            );
        }
        return new ApplicationException(
                ErrorCase.ALREADY_EXISTS,
                localMessage
        );
    }
}
