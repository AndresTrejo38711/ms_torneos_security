package com.uc.ms_torneos_security.repository;

import com.uc.ms_torneos_security.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmailAndIdNot(
            String email,
            Long id
    );

    @EntityGraph(attributePaths = {"profile"})
    Optional<User> findWithProfileById(Long id);

    // Usuarios activos que todavía no tienen asignado el rol indicado.
    @Query("""
            SELECT u FROM User u
            WHERE u.enabled = true
              AND NOT EXISTS (
                  SELECT ur FROM UserRole ur
                  WHERE ur.user = u
                    AND ur.role.id = :roleId
              )
            ORDER BY u.name
            """)
    List<User> findAvailableForRole(@Param("roleId") Long roleId);
}
