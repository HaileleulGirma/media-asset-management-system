package com.ena.mam.cassettecut;

import com.ena.mam.dto.request.CreateCassetteCutRequest;
import com.ena.mam.dto.request.filter.CassetteCutFilter;
import com.ena.mam.dto.response.CreateCassetteCutResponse;
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
public class CassetteCutController {

    private final CassetteCutService cassetteCutService;

    public CassetteCutController(CassetteCutService cassetteCutService) {
        this.cassetteCutService = cassetteCutService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/api/cassette-cuts")
    public CreateCassetteCutResponse create(
            @Valid @RequestBody CreateCassetteCutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateCassetteCutResponse response = cassetteCutService.create(request);

        log.info("AUDIT_CREATE | Module: CASSETTE_CUT | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedCutId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.CutId(), request);

        return response;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/api/cassette-cuts/{cutId}")
    public CreateCassetteCutResponse update(
            @PathVariable Long cutId,
            @Valid @RequestBody CreateCassetteCutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_UPDATE | Module: CASSETTE_CUT | Status: 200 | User: '{}' | IP: {} | Method: {} | CutId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), cutId, request);

        return cassetteCutService.update(cutId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/api/cassette-cuts/{cutId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long cutId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: CASSETTE_CUT | Status: 204 | User: '{}' | IP: {} | Method: {} | CutId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), cutId);

        cassetteCutService.delete(cutId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/cassette-cuts")
    public Page<CreateCassetteCutResponse> search(
            @RequestParam(required = false) Long digitizedMediaId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String searchTerm,
            Authentication authentication,
            HttpServletRequest httpRequest,
            Pageable pageable
    ) {
        CassetteCutFilter filter = new CassetteCutFilter(
                digitizedMediaId,
                categoryId,
                startDate,
                endDate,
                searchTerm
        );

        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        String clientIp = ClientIpUtil.getClientIp(httpRequest);
        String rawQueryString = httpRequest.getQueryString() != null ? httpRequest.getQueryString() : "<EMPTY>";

        log.info("AUDIT_SEARCH | Module: CASSETTE_CUT | Status: 200 | User: '{}' | IP: {} | Method: {} | RawQuery: '{}' | Filter: {} | Page: {}",
                currentUsername, clientIp, httpRequest.getMethod(), rawQueryString, filter, pageable.getPageNumber());

        return cassetteCutService.search(filter, pageable);
    }
}