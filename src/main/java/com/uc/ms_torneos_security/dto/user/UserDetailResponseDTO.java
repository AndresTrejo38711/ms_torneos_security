package com.uc.ms_torneos_security.dto.user;

import com.uc.ms_torneos_security.dto.profile.ProfileResponseDTO;
import com.uc.ms_torneos_security.entity.AuthProvider;
import lombok.Value;

@Value
public class UserDetailResponseDTO {

    Long id;

    String name;

    String email;

    AuthProvider authProvider;

    boolean enabled;

    boolean twoFactorEnabled;

    ProfileResponseDTO profile;
}
