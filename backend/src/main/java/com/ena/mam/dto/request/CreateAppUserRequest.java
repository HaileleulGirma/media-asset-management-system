package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAppUserRequest(

        @NotBlank(message = "Username cannot be empty.")
        String username,

        @NotBlank(message = "Password cannot be empty.")
        @Size(min = 4, message = "Password must be at least 4 characters long.")
        String password,

        @NotBlank(message = "Role cannot be empty.")
        @Pattern(
                regexp = "ADMIN|STAFF|VIEWER",
                message = "Role must be either ADMIN, STAFF or VIEWER."
        )
        String role
) {
}