package com.ena.mam.cameraman;

import com.ena.mam.dto.request.CreateCameramanRequest;
import com.ena.mam.dto.response.CreateCameramanResponse;
import com.ena.mam.exception.ResourceNotFoundException;
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

    public CreateCameramanResponse update(Long cameramanId, CreateCameramanRequest request){
        Cameraman cameraman = cameramanRepository.findById(cameramanId).orElseThrow(() -> new ResourceNotFoundException("Cameraman with id %d not found.".formatted(cameramanId)));
        cameraman.setCameramanName(request.cameramanName());
        cameraman.setActive(request.isActive());

        Cameraman savedCameraman = cameramanRepository.save(cameraman);

        return cameramanMapper.toResponse(savedCameraman);
    }

    public void delete(Long cameramanId){
        cameramanRepository.deleteById(cameramanId);
    }
}
