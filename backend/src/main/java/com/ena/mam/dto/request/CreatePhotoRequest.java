package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.Set;

public record CreatePhotoRequest(
        @NotEmpty(message = "At least one cameraman is required.")
        Set<Long> cameramanIds,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "File path is required")
        String filePath,

        @NotNull(message = "Number of files is required")
        @Positive(message = "Number of files must be greater than zero")
        Integer numberOfFiles,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be greater than zero")
        Double fileSizeMb,

        @NotNull(message = "Photo date is required")
        LocalDate photoDate,

        @NotNull(message = "Importer is required")
        Long importedBy,
        Long version
) {
}
