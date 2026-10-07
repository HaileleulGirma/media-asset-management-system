package com.ena.mam.dto.request.filter;

import java.time.LocalDate;

public record CassetteCutFilter(
        Long digitizedMediaId,
        Long categoryId,
        LocalDate cutDate,
        String searchTerm
) {
}
