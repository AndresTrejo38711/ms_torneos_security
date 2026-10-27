package com.uc.ms_torneos_security.dto.rolepermission;

import com.uc.ms_torneos_security.dto.permission.PermissionResponseDTO;
import lombok.Value;

@Value
public class RolePermissionResponseDTO {

    Long id;

    Long roleId;

    PermissionResponseDTO permission;
}
