package com.ena.mam.dto.request;

public record CreateLocationRequest(
        String locationName,
        Boolean isAbroad
        ) {
}
