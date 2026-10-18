package com.uc.ms_torneos_security.controller;

import com.uc.ms_torneos_security.dto.profile.CreateProfileDTO;
import com.uc.ms_torneos_security.dto.profile.ProfileResponseDTO;
import com.uc.ms_torneos_security.dto.profile.UpdateProfileDTO;
import com.uc.ms_torneos_security.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponseDTO create(
            @PathVariable Long userId,
            @Valid @RequestBody CreateProfileDTO dto) {
        return profileService.create(userId, dto);
    }

    @GetMapping
    public ProfileResponseDTO findByUserId(@PathVariable Long userId) {
        return profileService.findByUserId(userId);
    }

    @PutMapping
    public ProfileResponseDTO update(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileDTO dto) {
        return profileService.update(userId, dto);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        profileService.delete(userId);
    }
}
