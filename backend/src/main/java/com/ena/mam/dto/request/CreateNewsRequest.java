package com.ena.mam.dto.request;

import java.time.LocalDate;
import java.util.Set;

public record CreateNewsRequest(

        String title,
        Set<Long> location,
        LocalDate newsDate,
        Set<Long> cameramanIds,
        Set<Long> reporterIds,
        Set<Long> importerIds,
        Set<Long> ingestorIds,
        Integer numberOfFiles,
        Double totalSize

) {
}