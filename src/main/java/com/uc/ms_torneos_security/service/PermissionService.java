package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.permission.BasePermissionDTO;
import com.uc.ms_torneos_security.dto.permission.CreatePermissionDTO;
import com.uc.ms_torneos_security.dto.permission.EndpointRequestDTO;
import com.uc.ms_torneos_security.dto.permission.PermissionResponseDTO;
import com.uc.ms_torneos_security.dto.permission.UpdatePermissionDTO;
import com.uc.ms_torneos_security.entity.HttpMethodType;
import com.uc.ms_torneos_security.entity.Permission;
import com.uc.ms_torneos_security.entity.PermissionEndpoint;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.mapper.PermissionMapper;
import com.uc.ms_torneos_security.repository.PermissionEndpointRepository;
import com.uc.ms_torneos_security.repository.PermissionRepository;
import com.uc.ms_torneos_security.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    private final PermissionEndpointRepository permissionEndpointRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final PermissionMapper permissionMapper;

    public PermissionResponseDTO create(CreatePermissionDTO dto) {
        if (permissionRepository.existsByModelAndOperation(dto.getModel(), dto.getOperation())) {
            throw duplicatedOperation(dto);
        }
        permissionRepository.findFirstByModel(dto.getModel())
                .ifPresent(other -> validateSameModule(other, dto));
        validateEndpoints(dto, null);
        Permission permission = permissionMapper.toEntity(dto);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponseDTO(savedPermission);
    }

    public List<PermissionResponseDTO> findAll() {
        List<Permission> permissions = permissionRepository.findAllByOrderByModuleAscModelAscOperationAsc();
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
        permissionRepository.findFirstByModelAndIdNot(dto.getModel(), id)
                .ifPresent(other -> validateSameModule(other, dto));
        validateEndpoints(dto, id);
        permissionMapper.updateEntity(dto, permission);
        Permission updatedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponseDTO(updatedPermission);
    }

    public void delete(Long id) {
        Permission permission = findPermission(id);
        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "No se puede eliminar el permiso porque está asignado a roles"
            );
        }
        permissionRepository.delete(permission);
    }

    // Autorización (HU-SE-06 y HU-SE-08): se permite la petición si AL MENOS UNO de los roles
    // del usuario tiene un permiso con un endpoint que coincida con el método y la ruta.
    // La ruta real /api/users/15 coincide con el patrón guardado /api/users/{id}.
    public boolean hasPermission(Long userId, HttpMethodType method, String path) {
        PathContainer requestPath = PathContainer.parsePath(path);
        return permissionEndpointRepository.findAllowedUrls(userId, method).stream()
                .map(PathPatternParser.defaultInstance::parse)
                .anyMatch(pattern -> pattern.matches(requestPath));
    }

    private Permission findPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Permiso no encontrado con id: " + id
                ));
    }

    // Un recurso pertenece a un solo módulo: todas sus operaciones deben quedar en el mismo grupo de la matriz.
    private void validateSameModule(Permission other, BasePermissionDTO dto) {
        if (other.getModule() != dto.getModule()) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "El recurso " + dto.getModel() + " pertenece al módulo " + other.getModule()
            );
        }
    }

    // Cada endpoint (URL + método) pertenece a un solo permiso y no puede repetirse dentro del mismo.
    // permissionId es null al crear; al actualizar, se ignoran los endpoints del propio permiso.
    private void validateEndpoints(BasePermissionDTO dto, Long permissionId) {
        Set<String> seen = new HashSet<>();
        for (EndpointRequestDTO endpoint : dto.getEndpoints()) {
            String label = endpoint.getMethod() + " " + endpoint.getUrl();
            if (!seen.add(label)) {
                throw new ApplicationException(
                        ErrorCase.INVALID_OPERATION,
                        "El endpoint " + label + " está repetido en el permiso"
                );
            }
            validateUrlPattern(endpoint.getUrl());

            Optional<PermissionEndpoint> used = permissionId == null
                    ? permissionEndpointRepository.findFirstByUrlAndMethod(endpoint.getUrl(), endpoint.getMethod())
                    : permissionEndpointRepository.findFirstByUrlAndMethodAndPermissionIdNot(
                            endpoint.getUrl(), endpoint.getMethod(), permissionId);
            if (used.isPresent()) {
                Permission owner = used.get().getPermission();
                throw new ApplicationException(
                        ErrorCase.ALREADY_EXISTS,
                        "El endpoint " + label + " ya pertenece al permiso "
                                + owner.getModel() + " · " + owner.getOperation()
                );
            }
        }
    }

    // La URL se compara como patrón en cada petición, así que debe ser un patrón válido (llaves cerradas).
    private void validateUrlPattern(String url) {
        try {
            PathPatternParser.defaultInstance.parse(url);
        } catch (PatternParseException exception) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "La URL " + url + " no es un patrón válido (ej.: /api/users/{id})"
            );
        }
    }

    private ApplicationException duplicatedOperation(BasePermissionDTO dto) {
        return new ApplicationException(
                ErrorCase.ALREADY_EXISTS,
                "Ya existe un permiso " + dto.getOperation() + " para el recurso " + dto.getModel()
        );
    }
}
