package com.ena.mam.dto.response;

public record CreateCameramanResponse(
        Long cameramanId,
        String cameramanName,
        Boolean isActive
        ) {
}
