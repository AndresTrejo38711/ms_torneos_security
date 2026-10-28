package com.uc.ms_torneos_security.repository;

import com.uc.ms_torneos_security.entity.Operation;
import com.uc.ms_torneos_security.entity.Permission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByModelAndOperation(
            String model,
            Operation operation
    );

    boolean existsByModelAndOperationAndIdNot(
            String model,
            Operation operation,
            Long id
    );

    Optional<Permission> findByModelAndOperation(
            String model,
            Operation operation
    );

    // Otro permiso del mismo recurso: dice a qué módulo pertenece ya ese recurso.
    Optional<Permission> findFirstByModel(String model);

    Optional<Permission> findFirstByModelAndIdNot(
            String model,
            Long id
    );

    // Agrupados por módulo y recurso. MySQL ordena los enum alfabéticamente;
    // el orden de módulos y operaciones de la matriz lo fija RolePermissionMapper.
    @EntityGraph(attributePaths = {"endpoints"})
    List<Permission> findAllByOrderByModuleAscModelAscOperationAsc();

    // Permisos efectivos: basta con que AL MENOS UNO de los roles del usuario tenga el permiso.
    // Se calcula en cada consulta, así que los cambios en la matriz aplican de inmediato
    // a todos los usuarios que tengan el rol.
    @Query("""
            SELECT p FROM Permission p
            WHERE EXISTS (
                SELECT rp FROM RolePermission rp
                JOIN UserRole ur ON ur.role = rp.role
                WHERE rp.permission = p
                  AND ur.user.id = :userId
            )
            ORDER BY p.module, p.model, p.operation
            """)
    List<Permission> findEffectiveByUserId(@Param("userId") Long userId);
}
