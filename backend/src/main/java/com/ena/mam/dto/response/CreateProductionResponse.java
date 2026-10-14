package com.ena.mam.dto.response;

import java.time.LocalDate;

public record CreateProductionResponse(
        Long id,
        LocalDate date,
        String title,
        String filePath,
        Double fileSize,
        Integer durationSec,
        Long version
) {
}
