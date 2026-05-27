package com.ena.mam.dto.response;

public record CreateReporterResponse(
        Long id,
        String reporterName,
        Boolean isActive
        ) {

}
