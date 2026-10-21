package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import com.uc.ms_torneos_security.dto.userrole.AssignRoleRequestDTO;
import com.uc.ms_torneos_security.dto.userrole.RoleUserResponseDTO;
import com.uc.ms_torneos_security.dto.userrole.UserRoleResponseDTO;
import com.uc.ms_torneos_security.entity.Role;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.entity.UserRole;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.mapper.UserMapper;
import com.uc.ms_torneos_security.mapper.UserRoleMapper;
import com.uc.ms_torneos_security.repository.RoleRepository;
import com.uc.ms_torneos_security.repository.UserRepository;
import com.uc.ms_torneos_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserRoleMapper userRoleMapper;

    private final UserMapper userMapper;

    @Transactional
    public UserRoleResponseDTO assign(AssignRoleRequestDTO dto) {
        User user = findUser(dto.getUserId());
        Role role = findRole(dto.getRoleId());

        if (!user.isEnabled()) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "No se puede asignar un rol a un usuario desactivado"
            );
        }
        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El usuario ya tiene asignado ese rol"
            );
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        UserRole savedUserRole = userRoleRepository.save(userRole);
        return userRoleMapper.toResponseDTO(savedUserRole);
    }

    @Transactional(readOnly = true)
    public List<UserRoleResponseDTO> findAll() {
        List<UserRole> userRoles = userRoleRepository.findAll();
        return userRoleMapper.toResponseDTOList(userRoles);
    }

    @Transactional(readOnly = true)
    public List<UserRoleResponseDTO> findByUserId(Long userId) {
        findUser(userId);
        List<UserRole> userRoles = userRoleRepository.findAllByUserId(userId);
        return userRoleMapper.toResponseDTOList(userRoles);
    }

    @Transactional(readOnly = true)
    public List<RoleUserResponseDTO> findByRoleId(Long roleId) {
        findRole(roleId);
        List<UserRole> userRoles = userRoleRepository.findAllByRoleId(roleId);
        return userRoleMapper.toRoleUserResponseDTOList(userRoles);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAvailableUsers(Long roleId) {
        findRole(roleId);
        List<User> users = userRepository.findAvailableForRole(roleId);
        return userMapper.toResponseDTOList(users);
    }

    // Solo borra la asignación: el usuario y el rol siguen existiendo.
    @Transactional
    public void delete(Long userId, Long roleId) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "El usuario con id " + userId + " no tiene asignado el rol con id " + roleId
                ));
        userRoleRepository.delete(userRole);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Usuario no encontrado con id: " + id
                ));
    }

    private Role findRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Rol no encontrado con id: " + id
                ));
    }
}
