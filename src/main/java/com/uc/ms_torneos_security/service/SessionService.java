package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.entity.Session;
import com.uc.ms_torneos_security.entity.SessionStatus;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    @Value("${security.session.duration}")
    private Duration sessionDuration;

    // Cada inicio de sesión crea una sesión nueva: un usuario puede tener varias abiertas
    // (celular y PC) y volver a entrar después de cerrar o de que una venza.
    public Session create(User user, SessionStatus status) {
        LocalDateTime now = LocalDateTime.now();
        Session session = new Session();
        session.setTokenId(UUID.randomUUID().toString());
        session.setStatus(status);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plus(sessionDuration));
        session.setUser(user);
        return sessionRepository.save(session);
    }

    public boolean isActive(String tokenId) {
        return sessionRepository.isUsable(tokenId, SessionStatus.ACTIVE, LocalDateTime.now());
    }

    // Termina un login con 2FA: la sesión pendiente pasa a ACTIVE y sus 30 minutos cuentan desde ahora.
    public void activate(Session session) {
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(LocalDateTime.now().plus(sessionDuration));
        sessionRepository.save(session);
    }

    @Transactional
    public void close(String tokenId) {
        Session session = sessionRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.UNAUTHORIZED,
                        "La sesión no existe"
                ));
        close(session);
    }

    public void close(Session session) {
        session.setStatus(SessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional
    public void closeAllByUser(Long userId) {
        sessionRepository.closeAllByUserId(userId, SessionStatus.CLOSED, LocalDateTime.now());
    }
}
