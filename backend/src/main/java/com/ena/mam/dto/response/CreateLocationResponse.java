package com.ena.mam.dto.response;

public record CreateLocationResponse(
        Long locationId,
        String locationName,
        Boolean isAbroad
        ) {
}
