package com.uc.ms_torneos_security.mapper;

import com.uc.ms_torneos_security.dto.permission.CreatePermissionDTO;
import com.uc.ms_torneos_security.dto.permission.PermissionResponseDTO;
import com.uc.ms_torneos_security.dto.permission.UpdatePermissionDTO;
import com.uc.ms_torneos_security.entity.Permission;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PermissionMapper {

    public Permission toEntity(CreatePermissionDTO dto) {
        Permission permission = new Permission();
        permission.setModel(dto.getModel());
        permission.setOperation(dto.getOperation());
        permission.setUrl(dto.getUrl());
        permission.setMethod(dto.getMethod());
        return permission;
    }

    public void updateEntity(UpdatePermissionDTO dto, Permission permission) {
        permission.setModel(dto.getModel());
        permission.setOperation(dto.getOperation());
        permission.setUrl(dto.getUrl());
        permission.setMethod(dto.getMethod());
    }

    public PermissionResponseDTO toResponseDTO(Permission permission) {
        return new PermissionResponseDTO(
                permission.getId(),
                permission.getModel(),
                permission.getOperation(),
                permission.getUrl(),
                permission.getMethod()
        );
    }

    public List<PermissionResponseDTO> toResponseDTOList(List<Permission> permissions) {
        return permissions.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
