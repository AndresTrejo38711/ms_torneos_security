package com.uc.ms_torneos_security.controller;

import com.uc.ms_torneos_security.dto.user.CreateUserDTO;
import com.uc.ms_torneos_security.dto.user.UpdateUserDTO;
import com.uc.ms_torneos_security.dto.user.UserDetailResponseDTO;
import com.uc.ms_torneos_security.dto.user.UserResponseDTO;
import com.uc.ms_torneos_security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO create(@Valid @RequestBody CreateUserDTO dto) {
        return userService.create(dto);
    }

    @GetMapping
    public List<UserResponseDTO> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponseDTO findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping("/{id}/detail")
    public UserDetailResponseDTO findDetailById(@PathVariable Long id) {
        return userService.findDetailById(id);
    }

    @PutMapping("/{id}")
    public UserResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDTO dto) {
        return userService.update(id, dto);
    }

    @PatchMapping("/{id}/disable")
    public UserResponseDTO disable(@PathVariable Long id) {
        return userService.disable(id);
    }

    @PatchMapping("/{id}/enable")
    public UserResponseDTO enable(@PathVariable Long id) {
        return userService.enable(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
