package com.uc.ms_torneos_security.controller;

import com.uc.ms_torneos_security.dto.profile.CreateProfileDTO;
import com.uc.ms_torneos_security.dto.profile.ProfileResponseDTO;
import com.uc.ms_torneos_security.dto.profile.UpdateProfileDTO;
import com.uc.ms_torneos_security.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

// Perfil propio (HU-SE-02): cada usuario consulta, registra y actualiza SU perfil.
// Solo pide sesión, no un permiso. El dueño sale del token ("sub"), nunca de la URL:
// desde aquí nadie puede ver ni cambiar el perfil de otro usuario.
// /api/users/{userId}/profile sigue siendo la ruta del administrador (permiso "perfiles").
@RestController
@RequestMapping("/api/auth/me/profile")
@RequiredArgsConstructor
public class MyProfileController {

    private final ProfileService profileService;

    // 404 si el usuario todavía no registra su perfil.
    @GetMapping
    public ProfileResponseDTO find(@AuthenticationPrincipal Jwt jwt) {
        return profileService.findByUserId(userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponseDTO create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProfileDTO dto) {
        return profileService.create(userId(jwt), dto);
    }

    @PutMapping
    public ProfileResponseDTO update(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileDTO dto) {
        return profileService.update(userId(jwt), dto);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
