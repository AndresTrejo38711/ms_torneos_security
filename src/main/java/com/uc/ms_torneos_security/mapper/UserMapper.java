package com.uc.ms_torneos_security.mapper;

import com.uc.ms_torneos_security.dto.user.CreateUserDTO;
import com.uc.ms_torneos_security.dto.user.UpdateUserDTO;
import com.uc.ms_torneos_security.dto.user.UserDetailResponseDTO;
import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import com.uc.ms_torneos_security.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ProfileMapper profileMapper;

    // La contraseña no se copia aquí: el service la cifra con BCrypt antes de guardarla.
    public User toEntity(CreateUserDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return user;
    }

    public void updateEntity(UpdateUserDTO dto, User user) {
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
    }

    public UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAuthProvider(),
                user.isEnabled(),
                user.isTwoFactorEnabled()
        );
    }

    public UserDetailResponseDTO toDetailResponseDTO(User user) {
        return new UserDetailResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAuthProvider(),
                user.isEnabled(),
                user.isTwoFactorEnabled(),
                profileMapper.toResponseDTO(user.getProfile())
        );
    }

    public List<UserResponseDTO> toResponseDTOList(List<User> users) {
        return users.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
