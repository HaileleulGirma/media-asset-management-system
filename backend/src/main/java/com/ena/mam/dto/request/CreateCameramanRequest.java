package com.ena.mam.dto.request;

public record CreateCameramanRequest(
        String cameramanName,
        Boolean isActive
) {
}
