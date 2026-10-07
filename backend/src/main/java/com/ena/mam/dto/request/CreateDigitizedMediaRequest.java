package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDigitizedMediaRequest(
        Long DigitizedMediaId,
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "File path is required")
        String filePath,

        @NotBlank(message = "Identifier Category Id is required.")
        Long identifierCategoryId,

        @NotNull(message = "Identifier number is required.")
        Long identifierNumber,

        @NotNull(message = "Importer is required")
        Long importedBy,
        Long version

) {
}
