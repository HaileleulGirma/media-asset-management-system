package com.ena.mam.dto.response;

public record CreateProductionResponse(
        Long id,
        String title,
        String filePath,
        Double fileSize,
        Long version
) {
}
