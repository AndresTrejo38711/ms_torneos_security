package com.uc.ms_torneos_security.mapper;

import com.uc.ms_torneos_security.dto.permission.CreatePermissionDTO;
import com.uc.ms_torneos_security.dto.permission.EndpointRequestDTO;
import com.uc.ms_torneos_security.dto.permission.EndpointResponseDTO;
import com.uc.ms_torneos_security.dto.permission.PermissionResponseDTO;
import com.uc.ms_torneos_security.dto.permission.PermissionSummaryDTO;
import com.uc.ms_torneos_security.dto.permission.UpdatePermissionDTO;
import com.uc.ms_torneos_security.entity.HttpMethodType;
import com.uc.ms_torneos_security.entity.Permission;
import com.uc.ms_torneos_security.entity.PermissionEndpoint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PermissionMapper {

    public Permission toEntity(CreatePermissionDTO dto) {
        Permission permission = new Permission();
        permission.setModule(dto.getModule());
        permission.setModel(dto.getModel());
        permission.setOperation(dto.getOperation());
        dto.getEndpoints().forEach(endpoint -> addEndpoint(permission, endpoint));
        return permission;
    }

    // Conserva los endpoints que siguen en la lista, quita los que ya no vienen y agrega los nuevos.
    // Borrarlos todos y volver a crearlos chocaría con la regla de URL + método únicos.
    public void updateEntity(UpdatePermissionDTO dto, Permission permission) {
        permission.setModule(dto.getModule());
        permission.setModel(dto.getModel());
        permission.setOperation(dto.getOperation());

        Set<String> requested = dto.getEndpoints().stream()
                .map(endpoint -> key(endpoint.getMethod(), endpoint.getUrl()))
                .collect(Collectors.toSet());
        permission.getEndpoints().removeIf(
                endpoint -> !requested.contains(key(endpoint.getMethod(), endpoint.getUrl()))
        );

        Set<String> current = permission.getEndpoints().stream()
                .map(endpoint -> key(endpoint.getMethod(), endpoint.getUrl()))
                .collect(Collectors.toSet());
        dto.getEndpoints().stream()
                .filter(endpoint -> !current.contains(key(endpoint.getMethod(), endpoint.getUrl())))
                .forEach(endpoint -> addEndpoint(permission, endpoint));
    }

    public PermissionResponseDTO toResponseDTO(Permission permission) {
        return new PermissionResponseDTO(
                permission.getId(),
                permission.getModule(),
                permission.getModel(),
                permission.getOperation(),
                permission.getEndpoints().stream()
                        .map(endpoint -> new EndpointResponseDTO(endpoint.getUrl(), endpoint.getMethod()))
                        .toList()
        );
    }

    public List<PermissionResponseDTO> toResponseDTOList(List<Permission> permissions) {
        return permissions.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public PermissionSummaryDTO toSummaryDTO(Permission permission) {
        return new PermissionSummaryDTO(
                permission.getModule(),
                permission.getModel(),
                permission.getOperation()
        );
    }

    public List<PermissionSummaryDTO> toSummaryDTOList(List<Permission> permissions) {
        return permissions.stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    private void addEndpoint(Permission permission, EndpointRequestDTO dto) {
        PermissionEndpoint endpoint = new PermissionEndpoint();
        endpoint.setUrl(dto.getUrl());
        endpoint.setMethod(dto.getMethod());
        endpoint.setPermission(permission);
        permission.getEndpoints().add(endpoint);
    }

    private String key(HttpMethodType method, String url) {
        return method + " " + url;
    }
}
