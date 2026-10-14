package com.ena.mam.appuser;

import com.ena.mam.dto.request.CreateAppUserRequest;
import com.ena.mam.dto.request.UpdateAppUserRequest;
import com.ena.mam.dto.response.CreateAppUserResponse;
import com.ena.mam.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/appuser")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public CreateAppUserResponse create(
            @Valid @RequestBody CreateAppUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateAppUserResponse response = appUserService.create(request);

        log.info("AUDIT_CREATE | Module: APP_USER | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedAppUserId: {} | TargetUsername: '{}'",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.id(), request.username());

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{appUserId}")
    public CreateAppUserResponse update(
            @Valid @RequestBody UpdateAppUserRequest request,
            @PathVariable Long appUserId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateAppUserResponse response = appUserService.update(appUserId, request);

        log.info("AUDIT_UPDATE | Module: APP_USER | Status: 200 | User: '{}' | IP: {} | Method: {} | UpdatedAppUserId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), appUserId);

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{appUserId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long appUserId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: APP_USER | Status: 204 | User: '{}' | IP: {} | Method: {} | TargetAppUserId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), appUserId);

        appUserService.delete(appUserId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{appUserId}")
    public CreateAppUserResponse findById(
            @PathVariable Long appUserId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: APP_USER | Status: 200 | User: '{}' | IP: {} | Method: {} | TargetAppUserId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), appUserId);

        return appUserService.findById(appUserId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/")
    public List<CreateAppUserResponse> findAll(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: APP_USER | Status: 200 | User: '{}' | IP: {} | Method: {} | Action: FIND_ALL",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod());

        return appUserService.findAll();
    }
}