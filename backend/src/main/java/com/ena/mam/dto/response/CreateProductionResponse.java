package com.ena.mam.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductionResponse(
        Long id,
        LocalDate date,
        String title,
        String filePath,
        BigDecimal fileSize,
        Integer durationSec,
        Long version
) {
}
