package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.client.NotificationClient;
import com.uc.ms_torneos_security.dto.auth.ForgotPasswordRequestDTO;
import com.uc.ms_torneos_security.dto.auth.MessageResponseDTO;
import com.uc.ms_torneos_security.dto.auth.ResetPasswordRequestDTO;
import com.uc.ms_torneos_security.entity.AuthProvider;
import com.uc.ms_torneos_security.entity.PasswordResetCode;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.repository.PasswordResetCodeRepository;
import com.uc.ms_torneos_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

// Recuperación de contraseña (HU-SE-09): un código de 6 dígitos enviado por correo permite poner
// una contraseña nueva. Solo para cuentas con contraseña; las de Google, GitHub o Microsoft
// recuperan el acceso con su proveedor.
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String SUBJECT = "Recuperación de contraseña - Torneos";

    private final PasswordResetCodeRepository passwordResetCodeRepository;

    private final UserRepository userRepository;

    private final SessionService sessionService;

    private final NotificationClient notificationClient;

    private final PasswordEncoder passwordEncoder;

    @Value("${security.password-reset.code-duration}")
    private Duration codeDuration;

    // Responde lo mismo exista o no el correo, para no revelar qué correos están registrados:
    // la diferencia solo la ve el dueño del correo en su bandeja.
    // Transaccional: si el correo no se puede enviar, el código anterior sigue sirviendo.
    @Transactional
    public MessageResponseDTO forgot(ForgotPasswordRequestDTO dto) {
        userRepository.findByEmail(dto.getEmail())
                .ifPresent(this::sendInstructions);
        return new MessageResponseDTO(
                "Si el correo está registrado, te enviamos las instrucciones para recuperar el acceso"
        );
    }

    private void sendInstructions(User user) {
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            notificationClient.sendEmail(
                    user.getEmail(),
                    SUBJECT,
                    "Hola " + user.getName() + ",\n\n"
                            + "Recibimos una solicitud para recuperar la contraseña de tu cuenta, pero tu cuenta "
                            + "inicia sesión con " + user.getAuthProvider() + " y no tiene contraseña en la plataforma.\n"
                            + "Si no puedes entrar, recupera el acceso directamente con " + user.getAuthProvider() + ".\n\n"
                            + "Si no fuiste tú, ignora este correo."
            );
            return;
        }
        if (!user.isEnabled()) {
            notificationClient.sendEmail(
                    user.getEmail(),
                    SUBJECT,
                    "Hola " + user.getName() + ",\n\n"
                            + "Recibimos una solicitud para recuperar la contraseña de tu cuenta, pero tu cuenta "
                            + "está deshabilitada. Comunícate con el administrador de la plataforma.\n\n"
                            + "Si no fuiste tú, ignora este correo."
            );
            return;
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        passwordResetCodeRepository.deleteAllByUserId(user.getId());
        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setCodeHash(passwordEncoder.encode(code));
        resetCode.setExpiresAt(LocalDateTime.now().plus(codeDuration));
        resetCode.setUser(user);
        passwordResetCodeRepository.save(resetCode);

        notificationClient.sendEmail(
                user.getEmail(),
                SUBJECT,
                "Hola " + user.getName() + ",\n\n"
                        + "Tu código para restablecer la contraseña es: " + code + "\n\n"
                        + "Vence en " + codeDuration.toMinutes() + " minutos y solo se puede usar una vez.\n"
                        + "Si no pediste cambiar tu contraseña, ignora este correo: tu contraseña actual sigue funcionando."
        );
    }

    // Con el código correcto guarda la contraseña nueva y cierra todas las sesiones del usuario.
    // noRollbackFor: un código incorrecto también se guarda (cuenta como intento fallido).
    @Transactional(noRollbackFor = ApplicationException.class)
    public MessageResponseDTO reset(ResetPasswordRequestDTO dto) {
        // Correo que no existe, cuenta de un proveedor o que nunca pidió código: el mismo mensaje.
        User user = userRepository.findByEmail(dto.getEmail())
                .filter(found -> found.getAuthProvider() == AuthProvider.LOCAL)
                .orElseThrow(() -> invalid("El código no es válido: solicita uno nuevo"));
        PasswordResetCode resetCode = passwordResetCodeRepository.findFirstByUserIdOrderByIdDesc(user.getId())
                .orElseThrow(() -> invalid("El código no es válido: solicita uno nuevo"));

        if (resetCode.getUsedAt() != null) {
            throw invalid("Este código ya fue utilizado: solicita uno nuevo");
        }
        if (resetCode.getAttempts() >= MAX_ATTEMPTS) {
            throw invalid("Se agotaron los intentos de este código: solicita uno nuevo");
        }
        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw invalid("El código expiró: solicita uno nuevo");
        }
        if (!user.isEnabled()) {
            throw invalid("La cuenta está deshabilitada");
        }

        if (!passwordEncoder.matches(dto.getCode(), resetCode.getCodeHash())) {
            resetCode.setAttempts(resetCode.getAttempts() + 1);
            passwordResetCodeRepository.save(resetCode);

            int remaining = MAX_ATTEMPTS - resetCode.getAttempts();
            if (remaining <= 0) {
                throw invalid("Código incorrecto: se agotaron los intentos, solicita uno nuevo");
            }
            throw invalid(remaining == 1
                    ? "Código incorrecto: te queda 1 intento"
                    : "Código incorrecto: te quedan " + remaining + " intentos");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        resetCode.setUsedAt(LocalDateTime.now());
        passwordResetCodeRepository.save(resetCode);

        // Quien conocía la contraseña anterior o tenía un token abierto pierde el acceso.
        sessionService.closeAllByUser(user.getId());

        return new MessageResponseDTO("Contraseña actualizada: inicia sesión con tu nueva contraseña");
    }

    // 400 y no 401: no hay sesión de por medio, solo una solicitud que no es válida.
    private static ApplicationException invalid(String message) {
        return new ApplicationException(ErrorCase.INVALID_OPERATION, message);
    }
}
