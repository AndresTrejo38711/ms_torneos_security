package com.uc.ms_torneos_security.repository;

import com.uc.ms_torneos_security.entity.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    boolean existsByUserIdAndRoleId(
            Long userId,
            Long roleId
    );

    boolean existsByUserIdAndRoleName(
            Long userId,
            String roleName
    );

    // Usuarios que tienen el rol y todavía pueden entrar (cuenta activa).
    @Query("""
            SELECT COUNT(ur) FROM UserRole ur
            WHERE ur.role.name = :roleName
              AND ur.user.enabled = true
            """)
    long countEnabledUsersWithRole(@Param("roleName") String roleName);

    boolean existsByRoleId(Long roleId);

    @Override
    @EntityGraph(attributePaths = {"user", "role"})
    List<UserRole> findAll();

    @EntityGraph(attributePaths = {"user", "role"})
    List<UserRole> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "role"})
    List<UserRole> findAllByRoleId(Long roleId);

    Optional<UserRole> findByUserIdAndRoleId(
            Long userId,
            Long roleId
    );
}
