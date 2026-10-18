package com.uc.ms_torneos_security.dto.user;

import com.uc.ms_torneos_security.entity.AuthProvider;
import lombok.Value;

@Value
public class UserResponseDTO {

    Long id;

    String name;

    String email;

    AuthProvider authProvider;

    boolean enabled;

    boolean twoFactorEnabled;
}
