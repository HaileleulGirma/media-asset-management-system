package com.ena.mam.digitizedmedia;

import com.ena.mam.cassettecut.CassetteExportService;
import com.ena.mam.dto.request.CreateDigitizedMediaRequest;
import com.ena.mam.dto.request.filter.DigitizedMediaFilter;
import com.ena.mam.dto.response.CreateDigitizedMediaResponse;
import com.ena.mam.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Slf4j
@RestController
public class DigitizedMediaController {

    private final DigitizedMediaService digitizedMediaService;
    private final CassetteExportService cassetteExportService;

    public DigitizedMediaController(DigitizedMediaService digitizedMediaService, CassetteExportService cassetteExportService) {
        this.digitizedMediaService = digitizedMediaService;
        this.cassetteExportService = cassetteExportService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/api/digitized-media")
    public CreateDigitizedMediaResponse create(
            @Valid @RequestBody CreateDigitizedMediaRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateDigitizedMediaResponse response = digitizedMediaService.create(request);

        log.info("AUDIT_CREATE | Module: DIGITIZED_MEDIA | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedMediaId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.digitizedMediaId(), request);

        return response;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/api/digitized-media/{digitizedMediaId}")
    public CreateDigitizedMediaResponse update(
            @PathVariable Long digitizedMediaId,
            @Valid @RequestBody CreateDigitizedMediaRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_UPDATE | Module: DIGITIZED_MEDIA | Status: 200 | User: '{}' | IP: {} | Method: {} | MediaId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), digitizedMediaId, request);

        return digitizedMediaService.update(digitizedMediaId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/api/digitized-media/{digitizedMediaId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long digitizedMediaId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: DIGITIZED_MEDIA | Status: 204 | User: '{}' | IP: {} | Method: {} | MediaId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), digitizedMediaId);

        digitizedMediaService.delete(digitizedMediaId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/digitized-media")
    public Page<CreateDigitizedMediaResponse> search(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long importerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long identifierNumber,
            Authentication authentication,
            HttpServletRequest httpRequest,
            Pageable pageable
    ) {
        DigitizedMediaFilter filter = new DigitizedMediaFilter(
                startDate,
                endDate,
                importerId,
                categoryId,
                identifierNumber
        );

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        String clientIp = ClientIpUtil.getClientIp(httpRequest);
        String rawQueryString = httpRequest.getQueryString() != null ? httpRequest.getQueryString() : "<EMPTY>";

        log.info("AUDIT_SEARCH | Module: DIGITIZED_MEDIA | Status: 200 | User: '{}' | IP: {} | Method: {} | RawQuery: '{}' | Filter: {} | Page: {}",
                currentUsername, clientIp, httpRequest.getMethod(), rawQueryString, filter, pageable.getPageNumber());

        return digitizedMediaService.search(filter, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/digitized-media/export/csv")
    public void exportCsv(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long importerId,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) throws Exception {
        DigitizedMediaFilter filter = new DigitizedMediaFilter(startDate, endDate, importerId, null, null);

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        log.info("AUDIT_EXPORT_CSV | Module: DIGITIZED_MEDIA | User: '{}' | IP: {} | Filter: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), filter);

        if (cassetteExportService.rejectIfTooLarge(cassetteExportService.countCassettes(filter), response)) return;

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"cassette_staff_summary.csv\"");
        cassetteExportService.exportCassettesToCsv(filter, response.getWriter());
    }

// exportPdf: same pattern, @GetMapping("/api/digitized-media/export/pdf"),

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/digitized-media/export/pdf")
    public void exportPdf(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long importerId,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) throws Exception {
        DigitizedMediaFilter filter = new DigitizedMediaFilter(startDate, endDate, importerId, null, null);

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        log.info("AUDIT_EXPORT_PDF | Module: DIGITIZED_MEDIA | User: '{}' | IP: {} | Filter: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), filter);

        if (cassetteExportService.rejectIfTooLarge(cassetteExportService.countCassettes(filter), response)) return;

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"cassette_staff_summary.pdf\"");
        cassetteExportService.exportCassettesToPdf(filter, response.getOutputStream());
    }
}