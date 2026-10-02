package com.ena.mam.dto.request.filter;

import java.time.LocalDate;

public record PhotoFilter(
        LocalDate startDate,
        LocalDate endDate,
        String searchTerm,
        Long importerId
) {
    public LocalDate effectiveEndDate() {
        return (endDate != null) ? endDate : startDate;
    }
}