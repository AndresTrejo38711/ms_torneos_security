package com.uc.ms_torneos_security.repository;

import com.uc.ms_torneos_security.entity.TwoFactorCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TwoFactorCodeRepository extends JpaRepository<TwoFactorCode, Long> {

    // Trae también la sesión y su usuario: se necesitan para validar y generar el token.
    @EntityGraph(attributePaths = {"session", "session.user"})
    Optional<TwoFactorCode> findByChallengeId(String challengeId);
}
