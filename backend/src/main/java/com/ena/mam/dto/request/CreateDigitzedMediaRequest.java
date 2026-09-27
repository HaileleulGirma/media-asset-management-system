package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDigitzedMediaRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "File path is required")
        String filePath,

        @NotNull(message = "Importer is required")
        Long importedBy

) {
}
