package com.aviraj.user_service.user.controller;

import com.aviraj.user_service.common.response.ApiResponse;
import com.aviraj.user_service.user.dto.UserRequestDto;
import com.aviraj.user_service.user.dto.UserResponseDto;
import com.aviraj.user_service.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto dto) {
        UserResponseDto response =  service.createUser(dto);
        return new ApiResponse<>(true, "User created", response);
    }

    @GetMapping
    public List<UserResponseDto> getAllUsers() {
        return service.getAllUsers();
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponseDto> updateUser(@Valid @PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
        UserResponseDto response = service.updateUser(id, dto);
        return new ApiResponse<>(true, "User updated success", response);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@Valid @PathVariable Long id){
        service.deleteUser(id);
        return "User deleted successfully";
    }
}