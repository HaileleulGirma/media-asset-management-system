package com.ena.mam.dto.response;

import java.time.LocalDate;

public record CreateDigitizedMediaResponse(
        Long digitizedMediaId,
        LocalDate digitizedDate,
        String filePath,
        Long identifierCategoryId,
        Long identifierNumber,
        Long importerId,
        Boolean isPrivate,
        Long version
) {
}
