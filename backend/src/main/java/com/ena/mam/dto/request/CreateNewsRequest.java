package com.ena.mam.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;

public record CreateNewsRequest(

        @NotBlank
        String title,
        Set<Long> locationIds,
        @PastOrPresent(message = "Future dates not allowed.")
        LocalDate newsDate,
        Set<Long> cameramanIds,
        Set<Long> reporterIds,
        @NotBlank(message = "importer section cannot be blank.")
        Long importerId,
        Long ingestorId,
        @Min(value = 1, message = "number of files cannot be less than one.")
        @Max(value = 5000, message = "number of files cannot exceed 5000")
        Integer numberOfFiles,
        @Max(5000)
        @Positive(message = "total size cannot be less than zero.")
        Double totalSize

) {
}