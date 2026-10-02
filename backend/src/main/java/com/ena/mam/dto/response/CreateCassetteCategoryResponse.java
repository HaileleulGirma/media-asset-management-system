package com.ena.mam.dto.response;

public record CreateCassetteCategoryResponse(
        Long id,
        String categoryName,
        Long version
) {
}
