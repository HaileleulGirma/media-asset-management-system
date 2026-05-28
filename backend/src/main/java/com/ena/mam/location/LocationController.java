package com.ena.mam.location;

import com.ena.mam.dto.request.CreateLocationRequest;
import com.ena.mam.dto.response.CreateLocationResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationRepository locationRepository, LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/api/location/create")
    public CreateLocationResponse create(
            @RequestBody CreateLocationRequest request
            ){
        return locationService.create(request);
    }
}
