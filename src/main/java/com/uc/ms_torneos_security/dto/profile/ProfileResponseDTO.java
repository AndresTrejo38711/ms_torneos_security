package com.uc.ms_torneos_security.dto.profile;

import lombok.Value;

import java.time.LocalDate;

@Value
public class ProfileResponseDTO {

    Long id;

    String phone;

    String photo;

    LocalDate birthDate;

    Long userId;
}
