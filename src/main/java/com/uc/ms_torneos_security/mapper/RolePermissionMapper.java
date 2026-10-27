package com.uc.ms_torneos_security.mapper;

import com.uc.ms_torneos_security.dto.rolepermission.PermissionMatrixCellDTO;
import com.uc.ms_torneos_security.dto.rolepermission.PermissionMatrixModuleDTO;
import com.uc.ms_torneos_security.dto.rolepermission.PermissionMatrixResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.PermissionMatrixRowDTO;
import com.uc.ms_torneos_security.dto.rolepermission.PermissionRoleResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.RolePermissionResponseDTO;
import com.uc.ms_torneos_security.entity.ModuleType;
import com.uc.ms_torneos_security.entity.Operation;
import com.uc.ms_torneos_security.entity.Permission;
import com.uc.ms_torneos_security.entity.Role;
import com.uc.ms_torneos_security.entity.RolePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RolePermissionMapper {

    private final PermissionMapper permissionMapper;

    private final RoleMapper roleMapper;

    // Vista desde el rol: qué permisos tiene.
    public RolePermissionResponseDTO toResponseDTO(RolePermission rolePermission) {
        return new RolePermissionResponseDTO(
                rolePermission.getId(),
                rolePermission.getRole().getId(),
                permissionMapper.toResponseDTO(rolePermission.getPermission())
        );
    }

    public List<RolePermissionResponseDTO> toResponseDTOList(List<RolePermission> rolePermissions) {
        return rolePermissions.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // Vista desde el permiso: qué roles lo tienen.
    public PermissionRoleResponseDTO toPermissionRoleResponseDTO(RolePermission rolePermission) {
        return new PermissionRoleResponseDTO(
                rolePermission.getId(),
                rolePermission.getPermission().getId(),
                roleMapper.toResponseDTO(rolePermission.getRole())
        );
    }

    public List<PermissionRoleResponseDTO> toPermissionRoleResponseDTOList(List<RolePermission> rolePermissions) {
        return rolePermissions.stream()
                .map(this::toPermissionRoleResponseDTO)
                .toList();
    }

    // Arma la matriz del mockup: módulo -> recursos (filas) -> operaciones (columnas).
    // El EnumMap deja los módulos en el orden en que están declarados en ModuleType, y como
    // "permissions" llega ordenado por model, los recursos de cada módulo salen en orden alfabético.
    public PermissionMatrixResponseDTO toMatrixResponseDTO(
            Role role,
            List<Permission> permissions,
            Set<Long> assignedPermissionIds) {

        Map<ModuleType, Map<String, Map<Operation, Permission>>> permissionsByModule = new EnumMap<>(ModuleType.class);
        for (Permission permission : permissions) {
            permissionsByModule
                    .computeIfAbsent(permission.getModule(), module -> new LinkedHashMap<>())
                    .computeIfAbsent(permission.getModel(), model -> new EnumMap<>(Operation.class))
                    .put(permission.getOperation(), permission);
        }

        List<PermissionMatrixModuleDTO> modules = permissionsByModule.entrySet().stream()
                .map(entry -> toMatrixModuleDTO(entry.getKey(), entry.getValue(), assignedPermissionIds))
                .toList();

        return new PermissionMatrixResponseDTO(
                role.getId(),
                role.getName(),
                modules
        );
    }

    private PermissionMatrixModuleDTO toMatrixModuleDTO(
            ModuleType module,
            Map<String, Map<Operation, Permission>> permissionsByModel,
            Set<Long> assignedPermissionIds) {

        List<PermissionMatrixRowDTO> rows = permissionsByModel.entrySet().stream()
                .map(entry -> toMatrixRowDTO(entry.getKey(), entry.getValue(), assignedPermissionIds))
                .toList();

        return new PermissionMatrixModuleDTO(module, rows);
    }

    private PermissionMatrixRowDTO toMatrixRowDTO(
            String model,
            Map<Operation, Permission> permissionsByOperation,
            Set<Long> assignedPermissionIds) {

        List<PermissionMatrixCellDTO> cells = Arrays.stream(Operation.values())
                .map(operation -> {
                    Permission permission = permissionsByOperation.get(operation);
                    if (permission == null) {
                        return new PermissionMatrixCellDTO(operation, null, false);
                    }
                    return new PermissionMatrixCellDTO(
                            operation,
                            permission.getId(),
                            assignedPermissionIds.contains(permission.getId())
                    );
                })
                .toList();

        return new PermissionMatrixRowDTO(model, cells);
    }
}
