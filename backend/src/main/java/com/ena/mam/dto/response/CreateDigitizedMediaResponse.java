package com.ena.mam.dto.response;

public record CreateDigitizedMediaResponse(
        Long id,
        String title,
        String path,
        Long importerId,
        Long version
) {
}
