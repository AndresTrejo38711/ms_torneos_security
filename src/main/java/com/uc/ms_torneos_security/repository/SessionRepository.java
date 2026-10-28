package com.uc.ms_torneos_security.repository;

import com.uc.ms_torneos_security.entity.Session;
import com.uc.ms_torneos_security.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByTokenId(String tokenId);

    // Una sesión sirve solo si tiene el estado pedido, no ha vencido y su usuario sigue habilitado.
    @Query("""
            SELECT COUNT(s) > 0 FROM Session s
            WHERE s.tokenId = :tokenId
              AND s.status = :status
              AND s.expiresAt > :now
              AND s.user.enabled = true
            """)
    boolean isUsable(
            @Param("tokenId") String tokenId,
            @Param("status") SessionStatus status,
            @Param("now") LocalDateTime now
    );

    // Cierra de una vez todas las sesiones abiertas (ACTIVE o PENDING_2FA) de un usuario.
    @Modifying
    @Query("""
            UPDATE Session s
            SET s.status = :closed, s.closedAt = :now
            WHERE s.user.id = :userId
              AND s.status <> :closed
            """)
    int closeAllByUserId(
            @Param("userId") Long userId,
            @Param("closed") SessionStatus closed,
            @Param("now") LocalDateTime now
    );
}
