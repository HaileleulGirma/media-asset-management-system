package com.ena.mam.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record CreateNewsResponse(
        Long newsId,

        LocalDate newsDate,

        Set<Long> cameramanIds,

        Set<Long> reporterIds,

        String title,

        Set<Long> locationIds,

        String filePath,

        Boolean isPrivate,

        Long importerId,

        Long ingestorId,

        Integer numberOfFiles,

        BigDecimal totalSize,

        Long version
) {
}
