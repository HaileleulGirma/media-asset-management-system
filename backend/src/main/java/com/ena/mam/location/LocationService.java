package com.ena.mam.location;

import com.ena.mam.dto.request.CreateLocationRequest;
import com.ena.mam.dto.response.CreateLocationResponse;
import com.ena.mam.exception.ResourceNotFoundException;
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

    public CreateLocationResponse update(Long id, CreateLocationRequest request){
        Location location = locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Location with id %d not found.".formatted(id)));
        location.setLocationName(request.locationName());
        location.setAbroad(request.isAbroad());

        Location savedLocation = locationRepository.save(location);

        return locationMapper.toResponse(savedLocation);
    }

    public void delete(Long id){
        locationRepository.deleteById(id);
    }
}
