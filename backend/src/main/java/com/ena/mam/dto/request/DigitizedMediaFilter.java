package com.ena.mam.dto.request;

public record DigitizedMediaFilter(
        Long importerId,
        String searchTerm
) {
}