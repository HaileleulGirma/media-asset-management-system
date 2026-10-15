package com.ena.mam.production;

import com.ena.mam.dto.request.CreateProductionRequest;
import com.ena.mam.dto.request.filter.ProductionFilter;
import com.ena.mam.dto.response.CreateProductionResponse;
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
public class ProductionController {

    private final ProductionService service;

    public ProductionController(ProductionService service) {
        this.service = service;
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
}