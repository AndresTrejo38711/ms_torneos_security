package com.uc.ms_torneos_security.controller;

import com.uc.ms_torneos_security.dto.permission.PermissionResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.AssignPermissionRequestDTO;
import com.uc.ms_torneos_security.dto.rolepermission.PermissionMatrixResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.PermissionRoleResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.RolePermissionResponseDTO;
import com.uc.ms_torneos_security.dto.rolepermission.UpdatePermissionMatrixDTO;
import com.uc.ms_torneos_security.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolePermissionResponseDTO assign(@Valid @RequestBody AssignPermissionRequestDTO dto) {
        return rolePermissionService.assign(dto);
    }

    @GetMapping
    public List<RolePermissionResponseDTO> findAll() {
        return rolePermissionService.findAll();
    }

    @GetMapping("/roles/{roleId}")
    public List<RolePermissionResponseDTO> findByRoleId(@PathVariable Long roleId) {
        return rolePermissionService.findByRoleId(roleId);
    }

    @GetMapping("/roles/{roleId}/matrix")
    public PermissionMatrixResponseDTO findMatrix(@PathVariable Long roleId) {
        return rolePermissionService.findMatrix(roleId);
    }

    @PutMapping("/roles/{roleId}/matrix")
    public PermissionMatrixResponseDTO updateMatrix(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdatePermissionMatrixDTO dto) {
        return rolePermissionService.updateMatrix(roleId, dto);
    }

    @GetMapping("/permissions/{permissionId}")
    public List<PermissionRoleResponseDTO> findByPermissionId(@PathVariable Long permissionId) {
        return rolePermissionService.findByPermissionId(permissionId);
    }

    @GetMapping("/users/{userId}/effective")
    public List<PermissionResponseDTO> findEffectiveByUserId(@PathVariable Long userId) {
        return rolePermissionService.findEffectiveByUserId(userId);
    }

    @DeleteMapping("/{roleId}/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        rolePermissionService.delete(roleId, permissionId);
    }
}
