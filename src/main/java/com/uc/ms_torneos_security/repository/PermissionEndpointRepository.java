package com.uc.ms_torneos_security.repository;

import com.uc.ms_torneos_security.entity.HttpMethodType;
import com.uc.ms_torneos_security.entity.PermissionEndpoint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PermissionEndpointRepository extends JpaRepository<PermissionEndpoint, Long> {

    boolean existsByUrlAndMethod(
            String url,
            HttpMethodType method
    );

    // Trae también el permiso dueño del endpoint, para decir en el error a cuál pertenece.
    @EntityGraph(attributePaths = {"permission"})
    Optional<PermissionEndpoint> findFirstByUrlAndMethod(
            String url,
            HttpMethodType method
    );

    @EntityGraph(attributePaths = {"permission"})
    Optional<PermissionEndpoint> findFirstByUrlAndMethodAndPermissionIdNot(
            String url,
            HttpMethodType method,
            Long permissionId
    );

    // Patrones de URL que el usuario puede usar con ese método: basta con que
    // AL MENOS UNO de sus roles tenga el permiso dueño del endpoint.
    @Query("""
            SELECT e.url FROM PermissionEndpoint e
            WHERE e.method = :method
              AND EXISTS (
                  SELECT rp FROM RolePermission rp
                  JOIN UserRole ur ON ur.role = rp.role
                  WHERE rp.permission = e.permission
                    AND ur.user.id = :userId
              )
            """)
    List<String> findAllowedUrls(
            @Param("userId") Long userId,
            @Param("method") HttpMethodType method
    );
}
