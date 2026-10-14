package com.ena.mam.dto.response;

import java.time.LocalDate;

public record CreateAppUserResponse(
        Long id,
        String username,
        String fullname,
        Long role,
        LocalDate lastChanged
) {
}
