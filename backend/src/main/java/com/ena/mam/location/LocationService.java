package com.ena.mam.location;

import com.ena.mam.dto.request.CreateLocationRequest;
import com.ena.mam.dto.response.CreateLocationResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import com.ena.mam.reporter.ReporterMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {
    private final LocationMapper locationMapper;
    private final LocationRepository locationRepository;
    private final ReporterMapper reporterMapper;

    public LocationService(LocationMapper locationMapper, LocationRepository locationRepository, ReporterMapper reporterMapper) {
        this.locationMapper = locationMapper;
        this.locationRepository = locationRepository;
        this.reporterMapper = reporterMapper;
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

    public CreateLocationResponse findLocation(Long id){
        Location location = locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Location with id %d not found.".formatted(id)));
        return locationMapper.toResponse(location);
    }

    public List<CreateLocationResponse> findAll(){
        return locationRepository.findAll()
                .stream()
                .map(locationMapper::toResponse)
                .toList();
    }
}
