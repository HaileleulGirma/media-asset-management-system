package com.ena.mam.location;

import com.ena.mam.dto.request.CreateLocationRequest;
import com.ena.mam.dto.response.CreateLocationResponse;
import com.ena.mam.exception.DuplicateResourceException;
import com.ena.mam.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {
    private final LocationMapper locationMapper;
    private final LocationRepository locationRepository;

    public LocationService(LocationMapper locationMapper, LocationRepository locationRepository) {
        this.locationMapper = locationMapper;
        this.locationRepository = locationRepository;
    }

    public CreateLocationResponse create(CreateLocationRequest request){
        if (locationRepository.existsByLocationNameIgnoreCase(request.locationName())) {
            throw new DuplicateResourceException(
                    "A location named '%s' already exists.".formatted(request.locationName()));
        }
        Location location = locationMapper.toLocation(request);
        Location savedLocation = locationRepository.save(location);
        return locationMapper.toResponse(savedLocation);
    }

    public CreateLocationResponse update(Long id, CreateLocationRequest request){
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location with id %d not found.".formatted(id)));

        if (locationRepository.existsByLocationNameIgnoreCaseAndLocationIdNot(request.locationName(), id)) {
            throw new DuplicateResourceException(
                    "A location named '%s' already exists.".formatted(request.locationName()));
        }

        location.setLocationName(request.locationName());
        location.setAbroad(request.isAbroad());
        Location savedLocation = locationRepository.save(location);
        return locationMapper.toResponse(savedLocation);
    }

    public void delete(Long id){
        locationRepository.deleteById(id);
    }

    public CreateLocationResponse findLocation(Long id){
        Location location = locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Location with id %d not found.".formatted(id)));
        return locationMapper.toResponse(location);
    }

    public List<CreateLocationResponse> findAll(Boolean abroadOnly){
        List<Location> locations = (abroadOnly != null && abroadOnly)
                ? locationRepository.findByIsAbroadTrue()
                : locationRepository.findByIsAbroadFalse();

        return locations.stream()
                .map(locationMapper::toResponse)
                .toList();
    }
}
