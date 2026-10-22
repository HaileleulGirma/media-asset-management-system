package com.ena.mam.dto.request.filter;

import java.time.LocalDate;

public record CassetteCutFilter(
        Long digitizedMediaId,
        Long categoryId,
        LocalDate startDate,
        LocalDate endDate,
        String searchTerm
) {
    public LocalDate effectiveEndDate(){
        return (endDate != null) ? endDate : startDate;
    }
}
