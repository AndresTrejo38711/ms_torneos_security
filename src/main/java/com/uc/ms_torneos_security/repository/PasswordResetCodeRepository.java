package com.uc.ms_torneos_security.repository;

import com.uc.ms_torneos_security.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    // El último código que pidió el usuario: es el único que sirve.
    Optional<PasswordResetCode> findFirstByUserIdOrderByIdDesc(Long userId);

    // Al pedir un código nuevo, los anteriores dejan de existir.
    @Modifying
    @Query("DELETE FROM PasswordResetCode c WHERE c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
