package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAppUserRequest(
        @NotBlank(message = "Username cannot be empty.")
        String username,

        @NotBlank(message = "Username cannot be empty.")
        String fullName,

        @NotBlank(message = "Password cannot be empty.")
        @Size(min = 4, message = "Password must be at least 4 characters long.")
        String password,

        @NotNull(message = "Role cannot be empty.")
        @Positive(message = "Role must be a valid role ID.")
        Long role
) {
}