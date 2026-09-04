package com.ena.mam.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Set;

@ReporterOrCameramanRequired
public record CreateNewsRequest(
        @NotBlank
        String title,
        @NotEmpty(message = "At least one location is required.")
        Set<Long> locationIds,
        @NotNull(message = "News date is required.")
        @PastOrPresent(message = "Future dates not allowed.")
        LocalDate newsDate,
        Set<Long> cameramanIds,
        Set<Long> reporterIds,
        @NotBlank(message = "File path cannot be blank.")
        String filePath,
        @NotNull(message = "Importer is required.")
        Long importerId,
        Long ingestorId,
        @NotNull(message = "Number of files is required.")
        @Min(value = 1, message = "Number of files cannot be less than one.")
        @Max(value = 10000, message = "Number of files cannot exceed 10,000.")
        Integer numberOfFiles,
        @NotNull(message = "Total size is required.")
        @Max(value = 10000, message = "Total size cannot exceed 10TB.")
        @Positive(message = "Total size cannot be less than or equal to zero.")
        Double totalSize,
        Long version
) {
}