package com.ena.mam.dto.request.filter;

import java.time.LocalDate;

public record DigitizedMediaFilter(
        LocalDate startDate,
        LocalDate endDate,
        Long importerId,
        Long categoryId,
        Long identifierNumber
) {
    public LocalDate effectiveEndDate(){
        return (endDate != null) ? endDate : startDate;
    }
}