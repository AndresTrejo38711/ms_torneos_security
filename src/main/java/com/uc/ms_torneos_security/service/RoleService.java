package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.role.CreateRoleDTO;
import com.uc.ms_torneos_security.dto.role.RoleResponseDTO;
import com.uc.ms_torneos_security.dto.role.UpdateRoleDTO;
import com.uc.ms_torneos_security.entity.Role;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.mapper.RoleMapper;
import com.uc.ms_torneos_security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    private final RoleMapper roleMapper;

    public RoleResponseDTO create(CreateRoleDTO dto) {
        if (roleRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un rol con ese nombre"
            );
        }
        Role role = roleMapper.toEntity(dto);
        Role savedRole = roleRepository.save(role);
        return roleMapper.toResponseDTO(savedRole);
    }

    public List<RoleResponseDTO> findAll() {
        List<Role> roles = roleRepository.findAll();
        return roleMapper.toResponseDTOList(roles);
    }

    public RoleResponseDTO findById(Long id) {
        Role role = findRole(id);
        return roleMapper.toResponseDTO(role);
    }

    public RoleResponseDTO update(Long id, UpdateRoleDTO dto) {
        Role role = findRole(id);
        if (roleRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un rol con ese nombre"
            );
        }
        roleMapper.updateEntity(dto, role);
        Role updatedRole = roleRepository.save(role);
        return roleMapper.toResponseDTO(updatedRole);
    }

    // La validación "no eliminar un rol asignado a usuarios" se agrega con UserRole (HU-SE-04).
    public void delete(Long id) {
        Role role = findRole(id);
        roleRepository.delete(role);
    }

    private Role findRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Rol no encontrado con id: " + id
                ));
    }
}
