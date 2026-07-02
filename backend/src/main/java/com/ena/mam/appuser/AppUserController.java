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
    @PutMapping("/api/appuser/{appUserId}")
    public CreateAppUserResponse update(
            @Valid @RequestBody CreateAppUserRequest request,
            @PathVariable Long appUserId)
    {
        return appUserService.update(appUserId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/appuser/{appUserId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long appUserId)
    {
        appUserService.delete(appUserId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/appuser/{appUserId}")
    public CreateAppUserResponse findById(
            @PathVariable Long appUserId)
    {
        return appUserService.findById(appUserId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/appuser/")
    public List<CreateAppUserResponse> findAll()
    {
        return appUserService.findAll();
    }
}
