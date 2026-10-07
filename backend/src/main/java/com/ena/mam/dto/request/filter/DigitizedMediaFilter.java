package com.ena.mam.dto.request.filter;

public record DigitizedMediaFilter(
        Long importerId,
        Long categoryId,
        Long identifierNumber,
        String searchTerm
) {
}