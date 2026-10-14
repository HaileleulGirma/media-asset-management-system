package com.ena.mam.cameraman;

import com.ena.mam.dto.request.CreateCameramanRequest;
import com.ena.mam.dto.response.CreateCameramanResponse;
import com.ena.mam.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class CameramanController {

    private final CameramanService cameramanService;

    public CameramanController(CameramanService cameramanService) {
        this.cameramanService = cameramanService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/cameraman/create")
    public CreateCameramanResponse create(
            @Valid @RequestBody CreateCameramanRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateCameramanResponse response = cameramanService.create(request);

        log.info("AUDIT_CREATE | Module: CAMERAMAN | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedCameramanId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.cameramanId(), request);

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/cameraman/{cameramanId}")
    public CreateCameramanResponse update(
            @PathVariable Long cameramanId,
            @Valid @RequestBody CreateCameramanRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_UPDATE | Module: CAMERAMAN | Status: 200 | User: '{}' | IP: {} | Method: {} | CameramanId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), cameramanId, request);

        return cameramanService.update(cameramanId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/cameraman/{cameramanId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long cameramanId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: CAMERAMAN | Status: 204 | User: '{}' | IP: {} | Method: {} | CameramanId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), cameramanId);

        cameramanService.delete(cameramanId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/cameraman/{cameramanId}")
    public CreateCameramanResponse findById(
            @PathVariable Long cameramanId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: CAMERAMAN | Status: 200 | User: '{}' | IP: {} | Method: {} | CameramanId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), cameramanId);

        return cameramanService.findById(cameramanId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/cameraman")
    public List<CreateCameramanResponse> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: CAMERAMAN | Status: 200 | User: '{}' | IP: {} | Method: {} | Action: FIND_ALL | ActiveOnly: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), activeOnly);

        return cameramanService.findAll(activeOnly);
    }
}