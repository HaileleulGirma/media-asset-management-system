package com.ena.mam.location;

import com.ena.mam.dto.request.CreateLocationRequest;
import com.ena.mam.dto.response.CreateLocationResponse;
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
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationRepository locationRepository, LocationService locationService) {
        this.locationService = locationService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/location/create")
    public CreateLocationResponse create(
            @Valid @RequestBody CreateLocationRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateLocationResponse response = locationService.create(request);

        log.info("AUDIT_CREATE | Module: LOCATION | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedLocationId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.locationId(), request);

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/location/{locationId}")
    public CreateLocationResponse update(
            @PathVariable Long locationId,
            @Valid @RequestBody CreateLocationRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_UPDATE | Module: LOCATION | Status: 200 | User: '{}' | IP: {} | Method: {} | LocationId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), locationId, request);

        return locationService.update(locationId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/location/{locationId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long locationId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: LOCATION | Status: 204 | User: '{}' | IP: {} | Method: {} | LocationId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), locationId);

        locationService.delete(locationId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/location/{locationId}")
    public CreateLocationResponse findUsingId(
            @PathVariable Long locationId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: LOCATION | Status: 200 | User: '{}' | IP: {} | Method: {} | LocationId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), locationId);

        return locationService.findLocation(locationId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF','VIEWER')")
    @GetMapping("/api/location")
    public List<CreateLocationResponse> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean abroadOnly,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: LOCATION | Status: 200 | User: '{}' | IP: {} | Method: {} | Action: FIND_ALL | AbroadOnly: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), abroadOnly);

        return locationService.findAll(abroadOnly);
    }
}