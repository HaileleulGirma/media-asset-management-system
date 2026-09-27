package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProductionRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "File path is required")
        String filePath,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be greater than zero")
        Double fileSizeGb
) {
}
