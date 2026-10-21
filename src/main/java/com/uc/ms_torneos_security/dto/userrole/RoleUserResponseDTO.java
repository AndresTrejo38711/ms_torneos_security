package com.uc.ms_torneos_security.dto.userrole;

import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import lombok.Value;

@Value
public class RoleUserResponseDTO {

    Long id;

    Long roleId;

    UserResponseDTO user;
}
