package com.ena.mam.reporter;

import com.ena.mam.dto.request.CreateReporterRequest;
import com.ena.mam.dto.response.CreateReporterResponse;
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
public class ReporterController {

    private final ReporterService reporterService;

    public ReporterController(ReporterService reporterService) {
        this.reporterService = reporterService;
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/api/reporter/create")
    public CreateReporterResponse create(
            @Valid @RequestBody CreateReporterRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateReporterResponse response = reporterService.create(request);

        log.info("AUDIT_CREATE | Module: REPORTER | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedReporterId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.reporterId(), request);

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/reporter/{reporterId}")
    public CreateReporterResponse update(
            @PathVariable Long reporterId,
            @Valid @RequestBody CreateReporterRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_UPDATE | Module: REPORTER | Status: 200 | User: '{}' | IP: {} | Method: {} | ReporterId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), reporterId, request);

        return reporterService.update(reporterId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/reporter/{reporterId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long reporterId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: REPORTER | Status: 204 | User: '{}' | IP: {} | Method: {} | ReporterId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), reporterId);

        reporterService.delete(reporterId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/reporter/{reporterId}")
    public CreateReporterResponse findUsingId(
            @PathVariable Long reporterId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: REPORTER | Status: 200 | User: '{}' | IP: {} | Method: {} | ReporterId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), reporterId);

        return reporterService.findReporter(reporterId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/reporter")
    public List<CreateReporterResponse> findAll(
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: REPORTER | Status: 200 | User: '{}' | IP: {} | Method: {} | Action: FIND_ALL | ActiveOnly: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), activeOnly);

        return reporterService.findAll(activeOnly);
    }
}