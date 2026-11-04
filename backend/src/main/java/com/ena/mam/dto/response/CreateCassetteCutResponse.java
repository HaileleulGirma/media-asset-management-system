package com.ena.mam.dto.response;

import java.time.LocalDate;

public record CreateCassetteCutResponse(
        Long CutId,
        String CutTitle,
        LocalDate cutDate,
        Long digitizedMediaId,
        Long CassetteCategoryId,
        Long version,
        Long identifierCategoryId,
        Long identifierNumber,
        String filePath,
        Boolean isPrivate
) {
}
