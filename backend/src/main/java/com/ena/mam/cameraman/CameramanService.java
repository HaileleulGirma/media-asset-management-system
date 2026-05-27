package com.ena.mam.cameraman;

import com.ena.mam.dto.request.CreateCameramanRequest;
import com.ena.mam.dto.response.CreateCameramanResponse;
import org.springframework.stereotype.Service;

@Service
public class CameramanService {
    private final CameramanRepository cameramanRepository;
    private final CameramanMapper cameramanMapper;

    public CameramanService(CameramanRepository cameramanRepository, CameramanMapper cameramanMapper) {
        this.cameramanRepository = cameramanRepository;
        this.cameramanMapper = cameramanMapper;
    }

    public CreateCameramanResponse create(CreateCameramanRequest request){
        Cameraman cameraman = cameramanMapper.toCameraman(request);
        Cameraman savedCameraman = cameramanRepository.save(cameraman);
        return cameramanMapper.toResponse(savedCameraman);
    }
}
