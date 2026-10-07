package com.ena.mam.dto.response;

public record CreateDigitizedMediaResponse(
        Long digitizedMediaId,
        String filePath,
        Long identifierCategoryId,
        Long identifierNumber,
        Long importerId,
        Long version
) {
}
