package com.uc.ms_torneos_security.mapper;

import com.uc.ms_torneos_security.dto.profile.CreateProfileDTO;
import com.uc.ms_torneos_security.dto.profile.ProfileResponseDTO;
import com.uc.ms_torneos_security.dto.profile.UpdateProfileDTO;
import com.uc.ms_torneos_security.entity.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProfileMapper {

    // El usuario dueño del perfil lo asigna el service, no el mapper.
    public Profile toEntity(CreateProfileDTO dto) {
        Profile profile = new Profile();
        profile.setPhone(dto.getPhone());
        profile.setPhoto(dto.getPhoto());
        profile.setBirthDate(dto.getBirthDate());
        return profile;
    }

    public void updateEntity(UpdateProfileDTO dto, Profile profile) {
        profile.setPhone(dto.getPhone());
        profile.setPhoto(dto.getPhoto());
        profile.setBirthDate(dto.getBirthDate());
    }

    public ProfileResponseDTO toResponseDTO(Profile profile) {
        if (profile == null) {
            return null;
        }
        return new ProfileResponseDTO(
                profile.getId(),
                profile.getPhone(),
                profile.getPhoto(),
                profile.getBirthDate(),
                profile.getUser().getId()
        );
    }

    public List<ProfileResponseDTO> toResponseDTOList(List<Profile> profiles) {
        return profiles.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
