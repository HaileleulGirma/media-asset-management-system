package com.ena.mam.cassettecategory;

import com.ena.mam.dto.request.CreateCassetteCategoryRequest;
import com.ena.mam.dto.response.CreateCassetteCategoryResponse;
import com.ena.mam.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class CassetteCategoryController {

    private final CassetteCategoryService cassetteCategoryService;

    public CassetteCategoryController(CassetteCategoryService cassetteCategoryService) {
        this.cassetteCategoryService = cassetteCategoryService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/cassette-category")
    public CreateCassetteCategoryResponse create(
            @RequestBody CreateCassetteCategoryRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateCassetteCategoryResponse response = cassetteCategoryService.create(request);

        log.info("AUDIT_CREATE | Module: CASSETTE_CATEGORY | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedCategoryId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.id(), request);

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/cassette-category/{cassetteCategoryId}")
    public CreateCassetteCategoryResponse update(
            @PathVariable Long cassetteCategoryId,
            @RequestBody CreateCassetteCategoryRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_UPDATE | Module: CASSETTE_CATEGORY | Status: 200 | User: '{}' | IP: {} | Method: {} | CategoryId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), cassetteCategoryId, request);

        return cassetteCategoryService.update(cassetteCategoryId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/cassette-category/{cassetteCategoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long cassetteCategoryId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: CASSETTE_CATEGORY | Status: 204 | User: '{}' | IP: {} | Method: {} | CategoryId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), cassetteCategoryId);

        cassetteCategoryService.delete(cassetteCategoryId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/cassette-category/{cassetteCategoryId}")
    public CreateCassetteCategoryResponse findById(
            @PathVariable Long cassetteCategoryId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: CASSETTE_CATEGORY | Status: 200 | User: '{}' | IP: {} | Method: {} | CategoryId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), cassetteCategoryId);

        return cassetteCategoryService.findById(cassetteCategoryId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/cassette-category")
    public List<CreateCassetteCategoryResponse> findAll(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: CASSETTE_CATEGORY | Status: 200 | User: '{}' | IP: {} | Method: {} | Action: FIND_ALL",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod());

        return cassetteCategoryService.findAll();
    }
}