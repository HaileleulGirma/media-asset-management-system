package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateLocationRequest(
        @NotEmpty(message = "A location's name cannot be empty.")
        String locationName,
        @NotNull(message = "A location's abroad category cannot be empty.")
        Boolean isAbroad
        ) {
}
