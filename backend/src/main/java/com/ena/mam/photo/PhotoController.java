package com.ena.mam.photo;

import com.ena.mam.dto.request.CreatePhotoRequest;
import com.ena.mam.dto.request.filter.PhotoFilter;
import com.ena.mam.dto.response.CreatePhotoResponse;
import com.ena.mam.util.ClientIpUtil;
import com.ena.mam.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;

@Slf4j
@RestController
public class PhotoController {

    private final PhotoService photoService;
    private final PhotoExportService photoExportService;

    public PhotoController(PhotoService photoService, PhotoExportService photoExportService) {
        this.photoService = photoService;
        this.photoExportService = photoExportService;
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
            @RequestParam(required = false) Set<Long> cameramanIds,
            @RequestParam(required = false) Set<Long> locations,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Boolean isPrivate,
            @RequestParam(required = false) String searchTerm,
            Authentication authentication,
            HttpServletRequest httpRequest,
            Pageable pageable
    ) {
        PhotoFilter filter = new PhotoFilter(
                importerId,
                cameramanIds,
                locations,
                startDate,
                endDate,
                isPrivate,
                searchTerm
        );

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        String clientIp = ClientIpUtil.getClientIp(httpRequest);
        String rawQueryString = httpRequest.getQueryString() != null ? httpRequest.getQueryString() : "<EMPTY>";

        log.info("AUDIT_SEARCH | Module: PHOTO | Status: 200 | User: '{}' | IP: {} | Method: {} | RawQuery: '{}' | Filter: {} | Page: {}",
                currentUsername, clientIp, httpRequest.getMethod(), rawQueryString, filter, pageable.getPageNumber());

        return photoService.search(filter, pageable);
    }

    @GetMapping("/api/photos/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    public void exportCsv(
            @RequestParam(required = false) Long importerId,
            @RequestParam(required = false) Set<Long> cameramanIds,
            @RequestParam(required = false) Set<Long> locations,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Boolean isPrivate,
            @RequestParam(required = false) String searchTerm,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) throws Exception {
        boolean includeStaff = SecurityUtils.canCurrentUserViewPrivateContent();
        PhotoFilter filter = new PhotoFilter(
                includeStaff ? importerId : null,
                cameramanIds,
                locations,
                startDate,
                endDate,
                includeStaff ? isPrivate : Boolean.FALSE,
                searchTerm
        );

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        log.info("AUDIT_EXPORT_CSV | Module: PHOTO | User: '{}' | IP: {} | Filter: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), filter);

        if (rejectIfTooLarge(filter, response)) {
            return;
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"photos_export.csv\"");

        photoExportService.exportToCsv(filter, includeStaff, response.getWriter());
    }

    @GetMapping("/api/photos/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    public void exportPdf(
            @RequestParam(required = false) Long importerId,
            @RequestParam(required = false) Set<Long> cameramanIds,
            @RequestParam(required = false) Set<Long> locations,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Boolean isPrivate,
            @RequestParam(required = false) String searchTerm,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) throws Exception {
        boolean includeStaff = SecurityUtils.canCurrentUserViewPrivateContent();
        PhotoFilter filter = new PhotoFilter(
                includeStaff ? importerId : null,
                cameramanIds,
                locations,
                startDate,
                endDate,
                includeStaff ? isPrivate : Boolean.FALSE,
                searchTerm
        );

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        log.info("AUDIT_EXPORT_PDF | Module: PHOTO | User: '{}' | IP: {} | Filter: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), filter);

        if (rejectIfTooLarge(filter, response)) {
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"photos_export.pdf\"");

        photoExportService.exportToPdf(filter, includeStaff, response.getOutputStream());
    }

    // Exports are capped so a huge result set cannot tie up the server (PDF especially).
    // Checked before any headers or bytes are written, so the client gets a clean JSON error.
    private boolean rejectIfTooLarge(PhotoFilter filter, HttpServletResponse response) throws Exception {
        long total = photoExportService.countMatching(filter);
        if (total <= PhotoExportService.MAX_EXPORT_ROWS) {
            return false;
        }
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(STR."{\"message\":\"Too many results to export (\{total}). The limit is \{PhotoExportService.MAX_EXPORT_ROWS} - narrow your filters and try again.\"}");
        return true;
    }
}