package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.permission.BasePermissionDTO;
import com.uc.ms_torneos_security.dto.permission.CreatePermissionDTO;
import com.uc.ms_torneos_security.dto.permission.PermissionResponseDTO;
import com.uc.ms_torneos_security.dto.permission.UpdatePermissionDTO;
import com.uc.ms_torneos_security.entity.Permission;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.mapper.PermissionMapper;
import com.uc.ms_torneos_security.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    private final PermissionMapper permissionMapper;

    public PermissionResponseDTO create(CreatePermissionDTO dto) {
        if (permissionRepository.existsByModelAndOperation(dto.getModel(), dto.getOperation())) {
            throw duplicatedOperation(dto);
        }
        if (permissionRepository.existsByUrlAndMethod(dto.getUrl(), dto.getMethod())) {
            throw duplicatedEndpoint(dto);
        }
        Permission permission = permissionMapper.toEntity(dto);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponseDTO(savedPermission);
    }

    public List<PermissionResponseDTO> findAll() {
        List<Permission> permissions = permissionRepository.findAllByOrderByModelAscOperationAsc();
        return permissionMapper.toResponseDTOList(permissions);
    }

    public PermissionResponseDTO findById(Long id) {
        Permission permission = findPermission(id);
        return permissionMapper.toResponseDTO(permission);
    }

    public PermissionResponseDTO update(Long id, UpdatePermissionDTO dto) {
        Permission permission = findPermission(id);
        if (permissionRepository.existsByModelAndOperationAndIdNot(dto.getModel(), dto.getOperation(), id)) {
            throw duplicatedOperation(dto);
        }
        if (permissionRepository.existsByUrlAndMethodAndIdNot(dto.getUrl(), dto.getMethod(), id)) {
            throw duplicatedEndpoint(dto);
        }
        permissionMapper.updateEntity(dto, permission);
        Permission updatedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponseDTO(updatedPermission);
    }

    // La validación "no eliminar un permiso asignado a roles" se agrega con RolePermission (HU-SE-06).
    public void delete(Long id) {
        Permission permission = findPermission(id);
        permissionRepository.delete(permission);
    }

    private Permission findPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Permiso no encontrado con id: " + id
                ));
    }

    private ApplicationException duplicatedOperation(BasePermissionDTO dto) {
        return new ApplicationException(
                ErrorCase.ALREADY_EXISTS,
                "Ya existe un permiso " + dto.getOperation() + " para el recurso " + dto.getModel()
        );
    }

    private ApplicationException duplicatedEndpoint(BasePermissionDTO dto) {
        return new ApplicationException(
                ErrorCase.ALREADY_EXISTS,
                "Ya existe un permiso para " + dto.getMethod() + " " + dto.getUrl()
        );
    }
}
