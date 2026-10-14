package com.ena.mam.news;

import com.ena.mam.dto.request.CreateNewsRequest;
import com.ena.mam.dto.request.filter.NewsFilter;
import com.ena.mam.dto.response.CreateNewsResponse;
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
import java.util.Set;

@Slf4j
@RestController
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/api/news")
    public CreateNewsResponse create(
            @Valid @RequestBody CreateNewsRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateNewsResponse response = newsService.create(request);

        log.info("AUDIT_CREATE | Module: NEWS | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedNewsId: {} | Payload: {}",
                username, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.newsId(), request);

        return response;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/api/news/{newsId}")
    public CreateNewsResponse update(
            @PathVariable Long newsId,
            @Valid @RequestBody CreateNewsRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_UPDATE | Module: NEWS | Status: 200 | User: '{}' | IP: {} | Method: {} | NewsId: {} | Payload: {}",
                username, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), newsId, request);

        return newsService.update(newsId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/api/news/{newsId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long newsId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: NEWS | Status: 204 | User: '{}' | IP: {} | Method: {} | NewsId: {}",
                username, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), newsId);

        newsService.delete(newsId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/news")
    public Page<CreateNewsResponse> search(
            @RequestParam(required = false) Set<Long> reporterIds,
            @RequestParam(required = false) Set<Long> cameramanIds,
            @RequestParam(required = false) Set<Long> locationIds,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Long importerId,
            @RequestParam(required = false) Long ingestorId,
            @RequestParam(required = false) Boolean isPrivate,
            Authentication authentication,
            HttpServletRequest httpRequest,
            Pageable pageable) {

        NewsFilter filter = new NewsFilter(
                reporterIds, cameramanIds, locationIds, startDate, endDate,
                searchTerm, importerId, ingestorId, isPrivate
        );

        String username = (authentication != null) ? authentication.getName() : "SYSTEM";
        String clientIp = ClientIpUtil.getClientIp(httpRequest);
        String rawQueryString = httpRequest.getQueryString() != null ? httpRequest.getQueryString() : "<EMPTY>";

        log.info("AUDIT_SEARCH | Module: NEWS | Status: 200 | User: '{}' | IP: {} | Method: {} | RawQuery: '{}' | Filter: {} | Page: {}",
                username, clientIp, httpRequest.getMethod(), rawQueryString, filter, pageable.getPageNumber());

        return newsService.search(filter, pageable);
    }
}