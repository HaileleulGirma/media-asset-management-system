package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateCameramanRequest(

        @NotEmpty(message = "A cameraman name cannot be empty.")
        @Pattern(
                regexp = "^[A-Z][a-z]+\\s[A-Z][a-z]+$",
                message = "Cameraman's name must contain a first name and last name (e.g. John Smith)"
        )
        String cameramanName,
        @NotNull(message = "A cameraman's activity status cannot be empty.")
        Boolean isActive
) {
}
