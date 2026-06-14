package com.ena.mam.dto.request;

import java.time.LocalDate;
import java.util.Set;

public record NewsFilter(
        Set<Long> reporterIds,
        Set<Long> cameramanIds,
        Set<Long> locationIds,
        LocalDate startDate,
        LocalDate endDate,
        String searchTerm,
        Long importerId,
        Long ingestorId
) {
    public LocalDate effectiveEndDate() {
        return (endDate != null) ? endDate : startDate;}
}
