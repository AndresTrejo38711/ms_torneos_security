package com.uc.ms_torneos_security.dto.rolepermission;

import com.uc.ms_torneos_security.dto.role.RoleResponseDTO;
import lombok.Value;

@Value
public class PermissionRoleResponseDTO {

    Long id;

    Long permissionId;

    RoleResponseDTO role;
}
