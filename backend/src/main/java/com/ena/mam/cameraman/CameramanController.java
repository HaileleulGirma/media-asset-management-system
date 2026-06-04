package com.ena.mam.cameraman;

import com.ena.mam.dto.request.CreateCameramanRequest;
import com.ena.mam.dto.response.CreateCameramanResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/api/cameraman/{cameramanId}")
    public CreateCameramanResponse update(
            @PathVariable Long cameramanId, @Valid @RequestBody CreateCameramanRequest request
    ){
        return cameramanService.update(cameramanId, request);
    }

    @DeleteMapping("/api/cameraman/{cameramanId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long cameramanId
    ){
        cameramanService.delete(cameramanId);

        return ResponseEntity.noContent().build();
    }
}
