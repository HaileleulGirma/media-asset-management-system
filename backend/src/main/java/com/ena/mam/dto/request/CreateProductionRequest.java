package com.ena.mam.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateProductionRequest(
        @PastOrPresent(message = "Future dates not allowed.")
        LocalDate productionDate,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "File path is required")
        String filePath,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be greater than zero")
        Double fileSizeGb,

        @Min(value = 0, message = "Hours cannot be negative")
        Integer durationHours,

        @Min(value = 0, message = "Minutes cannot be negative")
        Integer durationMinutes,

        @Min(value = 0, message = "Seconds cannot be negative")
        Integer durationSeconds,
        Long version
) {
}
