package com.ena.mam.dto.response;

public record CreateAppUserResponse(
        Long id,
        String username,
        String fullname,
        Long role
) {
}
