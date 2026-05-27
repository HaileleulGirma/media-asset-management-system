package com.ena.mam.cameraman;

import com.ena.mam.dto.request.CreateCameramanRequest;
import com.ena.mam.dto.response.CreateCameramanResponse;
import org.springframework.stereotype.Component;

@Component
public class CameramanMapper {
    public Cameraman toCameraman(CreateCameramanRequest request){
        Cameraman cameraman = new Cameraman();

        cameraman.setCameramanName(request.cameramanName());
        cameraman.setActive(request.isActive());

        return cameraman;

    }

    public CreateCameramanResponse toResponse(Cameraman cameraman){
        return new CreateCameramanResponse(
                cameraman.getCameramanId(),
                cameraman.getCameramanName(),
                cameraman.getActive()
                );
    }
}
