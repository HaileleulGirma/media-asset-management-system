package com.ena.mam.dto.response;

public record CreateReporterResponse(
        Long reporterId,
        String reporterName,
        Boolean isActive
        ) {

}
