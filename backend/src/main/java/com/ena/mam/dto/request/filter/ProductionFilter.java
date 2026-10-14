package com.ena.mam.dto.request.filter;

import java.time.LocalDate;

public record ProductionFilter(
        String searchTerm,
        LocalDate startDate,
        LocalDate endDate
) {
    public LocalDate effectiveEndDate() {
        return (endDate != null) ? endDate : startDate;}
}
