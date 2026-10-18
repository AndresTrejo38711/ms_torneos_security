package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.profile.CreateProfileDTO;
import com.uc.ms_torneos_security.dto.profile.ProfileResponseDTO;
import com.uc.ms_torneos_security.dto.profile.UpdateProfileDTO;
import com.uc.ms_torneos_security.entity.Profile;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.mapper.ProfileMapper;
import com.uc.ms_torneos_security.repository.ProfileRepository;
import com.uc.ms_torneos_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    private final UserRepository userRepository;

    private final ProfileMapper profileMapper;

    public ProfileResponseDTO create(Long userId, CreateProfileDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Usuario no encontrado con id: " + userId
                ));

        if (profileRepository.existsByUserId(userId)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El usuario ya tiene un perfil"
            );
        }

        Profile profile = profileMapper.toEntity(dto);
        profile.setUser(user);

        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toResponseDTO(savedProfile);
    }

    public ProfileResponseDTO findByUserId(Long userId) {
        Profile profile = findProfile(userId);
        return profileMapper.toResponseDTO(profile);
    }

    public ProfileResponseDTO update(Long userId, UpdateProfileDTO dto) {
        Profile profile = findProfile(userId);
        profileMapper.updateEntity(dto, profile);
        Profile updatedProfile = profileRepository.save(profile);
        return profileMapper.toResponseDTO(updatedProfile);
    }

    public void delete(Long userId) {
        Profile profile = findProfile(userId);
        profileRepository.delete(profile);
    }

    private Profile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Perfil no encontrado para el usuario con id: " + userId
                ));
    }
}
