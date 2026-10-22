package com.uc.ms_torneos_security.repository;

import com.uc.ms_torneos_security.entity.HttpMethodType;
import com.uc.ms_torneos_security.entity.Operation;
import com.uc.ms_torneos_security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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

    boolean existsByUrlAndMethod(
            String url,
            HttpMethodType method
    );

    boolean existsByUrlAndMethodAndIdNot(
            String url,
            HttpMethodType method,
            Long id
    );

    // Agrupados por recurso, como se mostrarán en la matriz de la HU-SE-06.
    List<Permission> findAllByOrderByModelAscOperationAsc();
}
