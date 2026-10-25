package com.ena.mam.dto.request.filter;

import java.time.LocalDate;
import java.util.Set;

public record PhotoFilter(
        Long importerId,
        Set<Long> cameramanIds,
        Set<Long> locationIds,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isPrivate,
        String searchTerm
) {
    public LocalDate effectiveEndDate() {
        return (endDate != null) ? endDate : startDate;
    }
}