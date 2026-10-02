package com.ena.mam.dto.request;

public record CreateCassetteCategoryRequest(
        String categoryName,
        Long version
        ) {
}
