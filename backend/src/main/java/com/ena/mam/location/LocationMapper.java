package com.ena.mam.location;

import com.ena.mam.dto.request.CreateLocationRequest;
import com.ena.mam.dto.response.CreateLocationResponse;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public Location toLocation(CreateLocationRequest locationRequest){
        Location location = new Location();
        location.setLocationName(locationRequest.locationName());
        location.setAbroad(locationRequest.isAbroad());

        return location;
    }


    public CreateLocationResponse toResponse(Location location){
        return new CreateLocationResponse(
                location.getLocationId(),
                location.getLocationName(),
                location.getAbroad()
                );
    }
}
