package com.ena.mam.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record CreatePhotoResponse(
        Long photoId,
        Set<Long> cameramen,
        Set<Long> locations,
        String title,
        String filePath,
        Integer numberOfFiles,
        BigDecimal fileSizeMb,
        LocalDate photoDate,
        Boolean isPrivate,
        Long importedBy,
        Long version
) {
}