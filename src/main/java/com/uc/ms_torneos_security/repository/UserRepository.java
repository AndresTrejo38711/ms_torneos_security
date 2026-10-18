package com.uc.ms_torneos_security.repository;

import com.uc.ms_torneos_security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(
            String email,
            Long id
    );
}
