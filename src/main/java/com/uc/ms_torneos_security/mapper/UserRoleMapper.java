package com.uc.ms_torneos_security.mapper;

import com.uc.ms_torneos_security.dto.userrole.RoleUserResponseDTO;
import com.uc.ms_torneos_security.dto.userrole.UserRoleResponseDTO;
import com.uc.ms_torneos_security.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserRoleMapper {

    private final RoleMapper roleMapper;

    private final UserMapper userMapper;

    // Vista desde el usuario: qué roles tiene.
    public UserRoleResponseDTO toResponseDTO(UserRole userRole) {
        return new UserRoleResponseDTO(
                userRole.getId(),
                userRole.getUser().getId(),
                roleMapper.toResponseDTO(userRole.getRole())
        );
    }

    public List<UserRoleResponseDTO> toResponseDTOList(List<UserRole> userRoles) {
        return userRoles.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // Vista desde el rol: qué usuarios lo tienen.
    public RoleUserResponseDTO toRoleUserResponseDTO(UserRole userRole) {
        return new RoleUserResponseDTO(
                userRole.getId(),
                userRole.getRole().getId(),
                userMapper.toResponseDTO(userRole.getUser())
        );
    }

    public List<RoleUserResponseDTO> toRoleUserResponseDTOList(List<UserRole> userRoles) {
        return userRoles.stream()
                .map(this::toRoleUserResponseDTO)
                .toList();
    }
}
