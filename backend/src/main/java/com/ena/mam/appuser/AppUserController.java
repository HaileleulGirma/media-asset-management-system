package com.ena.mam.appuser;

import com.ena.mam.dto.request.CreateAppUserRequest;
import com.ena.mam.dto.response.CreateAppUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/appuser/create")
    public CreateAppUserResponse create(
            @Valid @RequestBody CreateAppUserRequest request)
    {
        return appUserService.create(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/appuser/{appuserId}")
    public CreateAppUserResponse update(
            @Valid @RequestBody CreateAppUserRequest request,
            @PathVariable Long appuserId)
    {
        return appUserService.update(appuserId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/appuser/{appuserId}")
    public ResponseEntity<Void> update(
            @PathVariable Long appuserId)
    {
        appUserService.delete(appuserId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/appuser/{appuserId}")
    public CreateAppUserResponse update(
            @PathVariable Long appuserId, @Valid @RequestBody CreateAppUserRequest request)
    {
        return appUserService.update(appuserId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/appuser/")
    public List<CreateAppUserResponse> findAll()
    {
        return appUserService.findAll();
    }
}
