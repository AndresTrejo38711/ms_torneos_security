package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.permission.PermissionResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.AssignPermissionRequestDTO;
import com.uc.ms_torneos_security.dto.rolepermission.PermissionMatrixResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.PermissionRoleResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.RolePermissionResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.UpdatePermissionMatrixDTO;
import com.uc.ms_torneos_security.entity.Permission;
import com.uc.ms_torneos_security.entity.Role;
import com.uc.ms_torneos_security.entity.RolePermission;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.mapper.PermissionMapper;
import com.uc.ms_torneos_security.mapper.RolePermissionMapper;
import com.uc.ms_torneos_security.repository.PermissionRepository;
import com.uc.ms_torneos_security.repository.RolePermissionRepository;
import com.uc.ms_torneos_security.repository.RoleRepository;
import com.uc.ms_torneos_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    private final UserRepository userRepository;

    private final RolePermissionMapper rolePermissionMapper;

    private final PermissionMapper permissionMapper;

    @Transactional
    public RolePermissionResponseDTO assign(AssignPermissionRequestDTO dto) {
        Role role = findRole(dto.getRoleId());
        Permission permission = findPermission(dto.getPermissionId());

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El rol ya tiene asignado ese permiso"
            );
        }

        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);

        RolePermission savedRolePermission = rolePermissionRepository.save(rolePermission);
        return rolePermissionMapper.toResponseDTO(savedRolePermission);
    }

    @Transactional(readOnly = true)
    public List<RolePermissionResponseDTO> findAll() {
        List<RolePermission> rolePermissions = rolePermissionRepository.findAll();
        return rolePermissionMapper.toResponseDTOList(rolePermissions);
    }

    @Transactional(readOnly = true)
    public List<RolePermissionResponseDTO> findByRoleId(Long roleId) {
        findRole(roleId);
        List<RolePermission> rolePermissions = rolePermissionRepository.findAllByRoleId(roleId);
        return rolePermissionMapper.toResponseDTOList(rolePermissions);
    }

    @Transactional(readOnly = true)
    public List<PermissionRoleResponseDTO> findByPermissionId(Long permissionId) {
        findPermission(permissionId);
        List<RolePermission> rolePermissions = rolePermissionRepository.findAllByPermissionId(permissionId);
        return rolePermissionMapper.toPermissionRoleResponseDTOList(rolePermissions);
    }

    // Todos los permisos del sistema, marcando solo los que el rol tiene asignados.
    @Transactional(readOnly = true)
    public PermissionMatrixResponseDTO findMatrix(Long roleId) {
        Role role = findRole(roleId);
        return buildMatrix(role);
    }

    // Acción "guardar" de la matriz: deja al rol exactamente con los permisos marcados.
    // Agrega los que se marcaron y retira los que se desmarcaron; los demás no se tocan.
    @Transactional
    public PermissionMatrixResponseDTO updateMatrix(Long roleId, UpdatePermissionMatrixDTO dto) {
        Role role = findRole(roleId);

        // El Set descarta ids repetidos, así no se intenta guardar dos veces la misma asociación.
        Set<Long> checkedIds = new LinkedHashSet<>(dto.getPermissionIds());
        List<Permission> checkedPermissions = permissionRepository.findAllById(checkedIds);
        if (checkedPermissions.size() != checkedIds.size()) {
            Set<Long> foundIds = checkedPermissions.stream()
                    .map(Permission::getId)
                    .collect(Collectors.toSet());
            List<Long> missingIds = checkedIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new ApplicationException(
                    ErrorCase.NOT_FOUND,
                    "Permisos no encontrados con id: " + missingIds
            );
        }

        List<RolePermission> currentRolePermissions = rolePermissionRepository.findAllByRoleId(roleId);
        Set<Long> currentIds = currentRolePermissions.stream()
                .map(rolePermission -> rolePermission.getPermission().getId())
                .collect(Collectors.toSet());

        List<RolePermission> toRemove = currentRolePermissions.stream()
                .filter(rolePermission -> !checkedIds.contains(rolePermission.getPermission().getId()))
                .toList();

        List<RolePermission> toAdd = checkedPermissions.stream()
                .filter(permission -> !currentIds.contains(permission.getId()))
                .map(permission -> {
                    RolePermission rolePermission = new RolePermission();
                    rolePermission.setRole(role);
                    rolePermission.setPermission(permission);
                    return rolePermission;
                })
                .toList();

        rolePermissionRepository.deleteAll(toRemove);
        rolePermissionRepository.saveAll(toAdd);

        return buildMatrix(role);
    }

    // Solo borra la asignación: el rol y el permiso siguen existiendo.
    @Transactional
    public void delete(Long roleId, Long permissionId) {
        RolePermission rolePermission = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "El rol con id " + roleId + " no tiene asignado el permiso con id " + permissionId
                ));
        rolePermissionRepository.delete(rolePermission);
    }

    // Unión de los permisos de todos los roles del usuario.
    @Transactional(readOnly = true)
    public List<PermissionResponseDTO> findEffectiveByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApplicationException(
                    ErrorCase.NOT_FOUND,
                    "Usuario no encontrado con id: " + userId
            );
        }
        List<Permission> permissions = permissionRepository.findEffectiveByUserId(userId);
        return permissionMapper.toResponseDTOList(permissions);
    }

    private PermissionMatrixResponseDTO buildMatrix(Role role) {
        List<Permission> permissions = permissionRepository.findAllByOrderByModuleAscModelAscOperationAsc();
        Set<Long> assignedIds = rolePermissionRepository.findAllByRoleId(role.getId()).stream()
                .map(rolePermission -> rolePermission.getPermission().getId())
                .collect(Collectors.toSet());
        return rolePermissionMapper.toMatrixResponseDTO(role, permissions, assignedIds);
    }

    private Role findRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Rol no encontrado con id: " + id
                ));
    }

    private Permission findPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Permiso no encontrado con id: " + id
                ));
    }
}
