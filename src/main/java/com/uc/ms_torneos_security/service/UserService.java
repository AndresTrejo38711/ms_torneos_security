package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.dto.user.CreateUserDTO;
import com.uc.ms_torneos_security.dto.user.UpdateUserDTO;
import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import com.uc.ms_torneos_security.entity.AuthProvider;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.mapper.UserMapper;
import com.uc.ms_torneos_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO create(CreateUserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un usuario con este email"
            );
        }
        User user = userMapper.toEntity(dto);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        User savedUser = userRepository.save(user);
        return userMapper.toResponseDTO(savedUser);
    }

    public List<UserResponseDTO> findAll() {
        List<User> users = userRepository.findAll();
        return userMapper.toResponseDTOList(users);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Usuario no encontrado con id: " + id
                ));
    }

    public UserResponseDTO findById(Long id) {
        User user = findUser(id);
        return userMapper.toResponseDTO(user);
    }

    public UserResponseDTO update(Long id, UpdateUserDTO dto) {
        User user = findUser(id);
        if (userRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El email pertenece a otro usuario"
            );
        }
        if (dto.getPassword() != null) {
            if (user.getAuthProvider() != AuthProvider.LOCAL) {
                throw new ApplicationException(
                        ErrorCase.INVALID_OPERATION,
                        "Un usuario autenticado con " + user.getAuthProvider()
                                + " no tiene contraseña en la plataforma"
                );
            }
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userMapper.updateEntity(dto, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
    }

    public UserResponseDTO disable(Long id) {
        User user = findUser(id);
        if (!user.isEnabled()) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "El usuario ya está desactivado"
            );
        }
        user.setEnabled(false);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
    }

    public UserResponseDTO enable(Long id) {
        User user = findUser(id);
        if (user.isEnabled()) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "El usuario ya está activo"
            );
        }
        user.setEnabled(true);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
    }

    public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
    }
}
