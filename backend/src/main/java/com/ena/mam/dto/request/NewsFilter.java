package com.ena.mam.dto.request;

import java.time.LocalDate;
import java.util.Set;

public record NewsFilter(

        String title,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        Set<Long> cameramanIds,
        Set<Long> reporterIds,
        Set<Long> importerIds,
        Set<Long> ingestorIds

) {
}