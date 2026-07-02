package com.ena.mam.appuser;

import com.ena.mam.dto.request.CreateAppUserRequest;
import com.ena.mam.dto.response.CreateAppUserResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserService {
    private final AppUserMapper appUserMapper;
    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserMapper appUserMapper, AppUserRepository appUserRepository) {
        this.appUserMapper = appUserMapper;
        this.appUserRepository = appUserRepository;
    }

    public CreateAppUserResponse create(CreateAppUserRequest request){
        AppUser appUser = appUserMapper.toAppUser(request);
        AppUser savedAppUser = appUserRepository.save(appUser);

        return appUserMapper.toResponse(savedAppUser);

    }

    public CreateAppUserResponse update(Long appUserId, CreateAppUserRequest request){
        AppUser appUser = appUserRepository.findById(appUserId).orElseThrow(() -> new ResourceNotFoundException("AppUser with id %d not found.".formatted(appUserId)));
        appUser.setFullName(request.fullName());
        appUser.setPassword(request.password());
        appUser.setUsername(request.username());

        AppUser savedAppUser = appUserRepository.save(appUser);

        return appUserMapper.toResponse(savedAppUser);
    }

    public void delete(Long id){
        appUserRepository.deleteById(id);
    }

    public CreateAppUserResponse findById(Long id){
        AppUser appUser = appUserRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("AppUser with id %d not found.".formatted(id)));
        return appUserMapper.toResponse(appUser);
    }

    public List<CreateAppUserResponse> findAll(){
        return appUserRepository.findAll()
                .stream()
                .map(appUserMapper::toResponse)
                .toList();
    }
}
