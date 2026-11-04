package com.ena.mam.production;

import com.ena.mam.dto.request.CreateProductionRequest;
import com.ena.mam.dto.request.filter.ProductionFilter;
import com.ena.mam.dto.response.CreateProductionResponse;
import com.ena.mam.util.ClientIpUtil;
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

@Slf4j
@RestController
public class ProductionController {

    private final ProductionService service;
    private final ProductionExportService exportService;

    public ProductionController(ProductionService service, ProductionExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/production")
    public CreateProductionResponse create(
            @Valid @RequestBody CreateProductionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateProductionResponse response = service.create(request);

        log.info("AUDIT_CREATE | Module: PRODUCTION | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedProductionId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.id(), request);

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/production/{productionId}")
    public CreateProductionResponse update(
            @PathVariable Long productionId,
            @Valid @RequestBody CreateProductionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateProductionResponse response = service.update(productionId, request);

        log.info("AUDIT_UPDATE | Module: PRODUCTION | Status: 200 | User: '{}' | IP: {} | Method: {} | ProductionId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), productionId, request);

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/production/{productionId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long productionId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        service.delete(productionId);

        log.info("AUDIT_DELETE | Module: PRODUCTION | Status: 204 | User: '{}' | IP: {} | Method: {} | ProductionId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), productionId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/production")
    public Page<CreateProductionResponse> search(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication,
            HttpServletRequest httpRequest,
            Pageable pageable
    ) {
        ProductionFilter filter = new ProductionFilter(searchTerm, startDate, endDate);
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        String clientIp = ClientIpUtil.getClientIp(httpRequest);
        String rawQueryString = httpRequest.getQueryString() != null ? httpRequest.getQueryString() : "<EMPTY>";

        log.info("AUDIT_SEARCH | Module: PRODUCTION | Status: 200 | User: '{}' | IP: {} | Method: {} | RawQuery: '{}' | Filter: {} | Page: {}",
                currentUsername, clientIp, httpRequest.getMethod(), rawQueryString, filter, pageable.getPageNumber());

        return service.search(filter, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/production/export/csv")
    public void exportCsv(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) throws Exception {
        ProductionFilter filter = new ProductionFilter(searchTerm, startDate, endDate);

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        log.info("AUDIT_EXPORT_CSV | Module: PRODUCTION | User: '{}' | IP: {} | Filter: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), filter);

        if (rejectIfTooLarge(filter, response)) {
            return;
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"production_export.csv\"");

        exportService.exportToCsv(filter, response.getWriter());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/production/export/pdf")
    public void exportPdf(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) throws Exception {
        ProductionFilter filter = new ProductionFilter(searchTerm, startDate, endDate);

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        log.info("AUDIT_EXPORT_PDF | Module: PRODUCTION | User: '{}' | IP: {} | Filter: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), filter);

        if (rejectIfTooLarge(filter, response)) {
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"production_export.pdf\"");

        exportService.exportToPdf(filter, response.getOutputStream());
    }

    // Exports are capped so a huge result set cannot tie up the server (PDF especially).
    // Checked before any headers or bytes are written, so the client gets a clean JSON error.
    private boolean rejectIfTooLarge(ProductionFilter filter, HttpServletResponse response) throws Exception {
        long total = exportService.countMatching(filter);
        if (total <= ProductionExportService.MAX_EXPORT_ROWS) {
            return false;
        }
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"Too many results to export (" + total + "). The limit is "
                + ProductionExportService.MAX_EXPORT_ROWS + " - narrow your filters and try again.\"}");
        return true;
    }
}