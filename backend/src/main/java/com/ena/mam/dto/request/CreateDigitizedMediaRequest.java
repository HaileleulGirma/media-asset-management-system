package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record CreateDigitizedMediaRequest(
        Long digitizedMediaId,

        @NotNull(message = "Digitized date is required.")
        @PastOrPresent(message = "Future dates are not allowed.")
        LocalDate digitizedDate,

        @NotBlank(message = "File path is required.")
        String filePath,

        @NotNull(message = "Identifier Category Id is required.")
        Long identifierCategoryId,

        @NotNull(message = "Identifier number is required.")
        Long identifierNumber,

        @NotNull(message = "Importer is required.")
        Long importedBy,

        @NotNull(message = "Privacy status is required.")
        Boolean isPrivate,
        Long version

) {
}
