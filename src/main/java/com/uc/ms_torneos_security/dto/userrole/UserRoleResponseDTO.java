package com.uc.ms_torneos_security.dto.userrole;

import com.uc.ms_torneos_security.dto.role.RoleResponseDTO;
import lombok.Value;

@Value
public class UserRoleResponseDTO {

    Long id;

    Long userId;

    RoleResponseDTO role;
}
