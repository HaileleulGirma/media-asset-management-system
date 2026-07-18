package com.ena.mam.location;

import com.ena.mam.dto.request.CreateLocationRequest;
import com.ena.mam.dto.response.CreateLocationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationRepository locationRepository, LocationService locationService) {
        this.locationService = locationService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/location/create")
    public CreateLocationResponse create(
            @Valid @RequestBody CreateLocationRequest request
            ){
        return locationService.create(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/location/{locationId}")
    public CreateLocationResponse update(
            @PathVariable @RequestParam Long locationId, @Valid @RequestBody CreateLocationRequest request
    ){
        return locationService.update(locationId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/location/{locationId}")
    public ResponseEntity<Void> delete(Long locationId){
        locationService.delete(locationId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/location/{locationId}")
    public CreateLocationResponse findUsingId(
            @PathVariable Long locationId
    )
    {
        return locationService.findLocation(locationId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/location")
    public List<CreateLocationResponse> findAll(
    ){
        return locationService.findAll();
    }
}
