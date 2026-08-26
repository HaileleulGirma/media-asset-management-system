package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateReporterRequest(
        @NotEmpty(message = "A Reporter's name cannot be empty.")
        @Pattern(
                regexp = "^[\\p{L}]+(?:[\\s'-][\\p{L}]+)+$",
                message = "Reporter's name must contain a first name and last name."
        )
        String reporterName,
        @NotNull(message = "Reporter activity status cannot be empty.")
        Boolean isActive) {

}
