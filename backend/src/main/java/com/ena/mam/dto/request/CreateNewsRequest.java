package com.ena.mam.dto.request;

import java.time.LocalDate;
import java.util.Set;

public record CreateNewsRequest(

        String title,
        Set<Long> locationIds,
        LocalDate newsDate,
        Set<Long> cameramanIds,
        Set<Long> reporterIds,
        Long importerId,
        Long ingestorId,
        Integer numberOfFiles,
        Double totalSize

) {
}