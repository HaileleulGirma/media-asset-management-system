package com.ena.mam.photo;

import com.ena.mam.dto.request.CreatePhotoRequest;
import com.ena.mam.dto.request.filter.PhotoFilter;
import com.ena.mam.dto.response.CreatePhotoResponse;
import com.ena.mam.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/api/photos")
    public CreatePhotoResponse create(
            @Valid @RequestBody CreatePhotoRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreatePhotoResponse response = photoService.create(request);

        log.info("AUDIT_CREATE | Module: PHOTO | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedPhotoId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.photoId(), request);

        return response;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/api/photos/{photoId}")
    public CreatePhotoResponse update(
            @PathVariable Long photoId,
            @Valid @RequestBody CreatePhotoRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_UPDATE | Module: PHOTO | Status: 200 | User: '{}' | IP: {} | Method: {} | PhotoId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), photoId, request);

        return photoService.update(photoId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/api/photos/{photoId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long photoId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: PHOTO | Status: 204 | User: '{}' | IP: {} | Method: {} | PhotoId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), photoId);

        photoService.delete(photoId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/photos")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    public Page<CreatePhotoResponse> search(
            @RequestParam(required = false) Long importerId,
            @RequestParam(required = false) Long cameramanId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String searchTerm,
            Authentication authentication,
            HttpServletRequest httpRequest,
            Pageable pageable
    ) {
        PhotoFilter filter = new PhotoFilter(
                importerId,
                cameramanId,
                startDate,
                endDate,
                searchTerm
        );

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        String clientIp = ClientIpUtil.getClientIp(httpRequest);
        String rawQueryString = httpRequest.getQueryString() != null ? httpRequest.getQueryString() : "<EMPTY>";

        log.info("AUDIT_SEARCH | Module: PHOTO | Status: 200 | User: '{}' | IP: {} | Method: {} | RawQuery: '{}' | Filter: {} | Page: {}",
                currentUsername, clientIp, httpRequest.getMethod(), rawQueryString, filter, pageable.getPageNumber());

        return photoService.search(filter, pageable);
    }
}