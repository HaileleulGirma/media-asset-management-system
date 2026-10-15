package com.ena.mam.appuser;

import com.ena.mam.approle.AppRole;
import com.ena.mam.dto.request.CreateAppUserRequest;
import com.ena.mam.dto.response.CreateAppUserResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AppUserMapper {

    public AppUser toAppUser(CreateAppUserRequest request, AppRole role, LocalDate lastChanged) {
        AppUser appUser = new AppUser();
        appUser.setUsername(request.username());
        appUser.setFullName(request.fullName());
        appUser.setPassword(request.password());
        appUser.setRole(role);
        appUser.setLastChanged(lastChanged);

        return appUser;
    }

    public CreateAppUserResponse toResponse(AppUser appUser) {
        return new CreateAppUserResponse(
                appUser.getUserId(),
                appUser.getUsername(),
                appUser.getFullName(),
                appUser.getRole().getRoleId(),
                appUser.getLastChanged()
        );
    }
}