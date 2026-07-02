package com.ena.mam.appuser;

import com.ena.mam.approle.AppRole;
import com.ena.mam.approle.AppRoleRepository;
import com.ena.mam.dto.request.CreateAppUserRequest;
import com.ena.mam.dto.response.CreateAppUserResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AppUserMapper {

    private final AppRoleRepository appRoleRepository;

    public AppUserMapper(AppRoleRepository appRoleRepository) {
        this.appRoleRepository = appRoleRepository;
    }

    public AppUser toAppUser(CreateAppUserRequest request) {
        AppRole role = appRoleRepository.findById(request.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.role()));

        AppUser appUser = new AppUser();
        appUser.setUsername(request.username());
        appUser.setPassword(request.password());
        appUser.setRole(role);
        return appUser;
    }

    public CreateAppUserResponse toResponse(AppUser appUser) {
        return new CreateAppUserResponse(
                appUser.getUserId(),
                appUser.getUsername(),
                appUser.getRole().getRoleId()
        );
    }
}