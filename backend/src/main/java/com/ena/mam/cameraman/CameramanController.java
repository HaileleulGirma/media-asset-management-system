package com.ena.mam.cameraman;

import com.ena.mam.dto.request.CreateCameramanRequest;
import com.ena.mam.dto.response.CreateCameramanResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CameramanController {
    private final CameramanService cameramanService;

    public CameramanController(CameramanService cameramanService) {
        this.cameramanService = cameramanService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/cameraman/create")
    public CreateCameramanResponse create(
           @Valid @RequestBody CreateCameramanRequest request
            )
    {
        return cameramanService.create(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/cameraman/{cameramanId}")
    public CreateCameramanResponse update(
            @PathVariable Long cameramanId, @Valid @RequestBody CreateCameramanRequest request
    ){
        return cameramanService.update(cameramanId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/cameraman/{cameramanId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long cameramanId
    ){
        cameramanService.delete(cameramanId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/cameraman/{cameramanId}")
    public CreateCameramanResponse findById(
            @PathVariable Long cameramanId
    )
    {
        return cameramanService.findById(cameramanId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/cameraman")
    public List<CreateCameramanResponse> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ){
        return cameramanService.findAll(activeOnly);
    }
}
