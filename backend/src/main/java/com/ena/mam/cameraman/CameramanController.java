package com.ena.mam.cameraman;

import com.ena.mam.dto.request.CreateCameramanRequest;
import com.ena.mam.dto.response.CreateCameramanResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CameramanController {
    private final CameramanService cameramanService;

    public CameramanController(CameramanService cameramanService) {
        this.cameramanService = cameramanService;
    }

    @PostMapping("/api/cameraman/create")
    public CreateCameramanResponse create(
           @Valid @RequestBody CreateCameramanRequest request
            )
    {
        return cameramanService.create(request);
    }
}
