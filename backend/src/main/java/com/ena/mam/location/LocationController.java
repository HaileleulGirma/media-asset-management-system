package com.ena.mam.location;

import com.ena.mam.dto.request.CreateLocationRequest;
import com.ena.mam.dto.response.CreateLocationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationRepository locationRepository, LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/api/location/create")
    public CreateLocationResponse create(
            @Valid @RequestBody CreateLocationRequest request
            ){
        return locationService.create(request);
    }

    @PutMapping("/api/location/{locationId}")
    public CreateLocationResponse update(
            @PathVariable @RequestParam Long locationId, @Valid @RequestBody CreateLocationRequest request
    ){
        return locationService.update(locationId, request);
    }

    @DeleteMapping("api/location/{locationId}")
    public ResponseEntity<Void> delete(Long locationId){
        locationService.delete(locationId);

        return ResponseEntity.noContent().build();
    }
}
