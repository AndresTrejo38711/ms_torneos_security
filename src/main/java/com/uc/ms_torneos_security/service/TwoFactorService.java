package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.client.NotificationClient;
import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import com.uc.ms_torneos_security.entity.AuthProvider;
import com.uc.ms_torneos_security.entity.Session;
import com.uc.ms_torneos_security.entity.SessionStatus;
import com.uc.ms_torneos_security.entity.TwoFactorCode;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.mapper.UserMapper;
import com.uc.ms_torneos_security.repository.TwoFactorCodeRepository;
import com.uc.ms_torneos_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

// Segundo factor de autenticación (HU-SE-10): códigos de 6 dígitos enviados por correo,
// válidos por pocos minutos, de un solo uso y con intentos limitados.
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TwoFactorCodeRepository twoFactorCodeRepository;

    private final UserRepository userRepository;

    private final SessionService sessionService;

    private final NotificationClient notificationClient;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;

    @Value("${security.two-factor.code-duration}")
    private Duration codeDuration;

    // Crea el código de una sesión PENDING_2FA y lo envía al correo del usuario.
    // Devuelve el challengeId con el que el cliente completará el inicio de sesión.
    public String sendCode(Session session) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        TwoFactorCode twoFactorCode = new TwoFactorCode();
        twoFactorCode.setChallengeId(UUID.randomUUID().toString());
        twoFactorCode.setCodeHash(passwordEncoder.encode(code));
        twoFactorCode.setExpiresAt(LocalDateTime.now().plus(codeDuration));
        twoFactorCode.setSession(session);
        twoFactorCodeRepository.save(twoFactorCode);

        User user = session.getUser();
        notificationClient.sendEmail(
                user.getEmail(),
                "Código de verificación - Torneos",
                "Hola " + user.getName() + ",\n\n"
                        + "Tu código para iniciar sesión es: " + code + "\n\n"
                        + "Vence en " + codeDuration.toMinutes() + " minutos y solo se puede usar una vez.\n"
                        + "Si no fuiste tú quien intentó iniciar sesión, cambia tu contraseña."
        );

        return twoFactorCode.getChallengeId();
    }

    // Valida el código y devuelve la sesión que completa. Cada fallo queda registrado, así que quien
    // la llame debe tener una transacción que no se deshaga con ApplicationException.
    public Session checkCode(String challengeId, String code) {
        TwoFactorCode twoFactorCode = twoFactorCodeRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> unauthorized("El código no es válido: inicia sesión de nuevo"));
        Session session = twoFactorCode.getSession();

        if (twoFactorCode.getUsedAt() != null) {
            throw unauthorized("Este código ya fue utilizado");
        }
        if (session.getStatus() != SessionStatus.PENDING_2FA) {
            throw unauthorized("Este inicio de sesión ya no está pendiente: inicia sesión de nuevo");
        }
        if (twoFactorCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionService.close(session);
            throw unauthorized("El código expiró: inicia sesión de nuevo");
        }
        if (!session.getUser().isEnabled()) {
            sessionService.close(session);
            throw unauthorized("La cuenta está deshabilitada");
        }

        if (!passwordEncoder.matches(code, twoFactorCode.getCodeHash())) {
            twoFactorCode.setAttempts(twoFactorCode.getAttempts() + 1);
            twoFactorCodeRepository.save(twoFactorCode);

            int remaining = MAX_ATTEMPTS - twoFactorCode.getAttempts();
            if (remaining <= 0) {
                sessionService.close(session);
                throw unauthorized("Código incorrecto: se agotaron los intentos, inicia sesión de nuevo");
            }
            throw unauthorized(remaining == 1
                    ? "Código incorrecto: te queda 1 intento"
                    : "Código incorrecto: te quedan " + remaining + " intentos");
        }

        twoFactorCode.setUsedAt(LocalDateTime.now());
        twoFactorCodeRepository.save(twoFactorCode);
        return session;
    }

    public UserResponseDTO enable(Long userId, String password) {
        User user = findLocalUser(userId, password);
        if (user.isTwoFactorEnabled()) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "La verificación en dos pasos ya está activada"
            );
        }
        user.setTwoFactorEnabled(true);
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    public UserResponseDTO disable(Long userId, String password) {
        User user = findLocalUser(userId, password);
        if (!user.isTwoFactorEnabled()) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "La verificación en dos pasos ya está desactivada"
            );
        }
        user.setTwoFactorEnabled(false);
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    // El 2FA de la plataforma solo existe para cuentas con contraseña: en las de Google, GitHub
    // o Microsoft la seguridad del acceso la pone el proveedor.
    private User findLocalUser(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Usuario no encontrado con id: " + userId
                ));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "Tu cuenta inicia sesión con " + user.getAuthProvider()
                            + ": la verificación en dos pasos la maneja ese proveedor"
            );
        }
        // 400 y no 401: la sesión sigue siendo válida, solo la contraseña está mal.
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "La contraseña no es correcta"
            );
        }
        return user;
    }

    private static ApplicationException unauthorized(String message) {
        return new ApplicationException(ErrorCase.UNAUTHORIZED, message);
    }
}
