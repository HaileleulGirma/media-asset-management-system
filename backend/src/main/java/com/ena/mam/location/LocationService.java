package com.ena.mam.location;

import com.ena.mam.dto.request.CreateLocationRequest;
import com.ena.mam.dto.response.CreateLocationResponse;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private final LocationMapper locationMapper;
    private final LocationRepository locationRepository;

    public LocationService(LocationMapper locationMapper, LocationRepository locationRepository) {
        this.locationMapper = locationMapper;
        this.locationRepository = locationRepository;
    }

    public CreateLocationResponse create(CreateLocationRequest request){
        Location location = locationMapper.toLocation(request);
        Location savedLocation = locationRepository.save(location);

        return locationMapper.toResponse(savedLocation);
    }
}
