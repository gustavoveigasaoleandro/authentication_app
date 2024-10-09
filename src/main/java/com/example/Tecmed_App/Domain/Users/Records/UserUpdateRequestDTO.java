package com.example.Tecmed_App.Domain.Users.Records;

import com.example.Tecmed_App.Enums.Role;

public record UserUpdateRequestDTO(
        Long id,
        String username,
        Role role) {

    public UserUpdateRequestDTO {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (role != Role.TECHNICIAN && role != Role.MANAGER) {
            throw new IllegalArgumentException("Role must be TECHNICIAN or MANAGER and cannot be null");
        }
    }
}