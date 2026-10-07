package com.ena.mam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record CreateCassetteCutRequest(
        Long cutId,
        @NotBlank(message = "The Cut must have a title.")
        String cutTitle,
        @PastOrPresent(message = "Future dates not allowed.")
        LocalDate cutDate,
        @NotEmpty(message = "Digitized media identifier is required.")
        Long digitizedMediaId,
        @NotEmpty(message = "Cassette category identifier is required.")
        Long cassetteCategoryId,
        Long version

) {
}
