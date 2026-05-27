package com.ena.mam.dto.request;

public record CreateReporterRequest(
        String reporterName,
        Boolean isActive) {

}
