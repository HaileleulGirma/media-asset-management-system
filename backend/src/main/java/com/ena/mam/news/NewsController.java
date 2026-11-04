package com.ena.mam.news;

import com.ena.mam.dto.request.CreateNewsRequest;
import com.ena.mam.dto.request.filter.NewsFilter;
import com.ena.mam.dto.response.CreateNewsResponse;
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
public class NewsController {

    private final NewsService newsService;
    private final NewsExportService newsExportService;

    public NewsController(NewsService newsService, NewsExportService newsExportService) {
        this.newsService = newsService;
        this.newsExportService = newsExportService;
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

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/news/export/csv")
    public void exportCsv(
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
            HttpServletResponse response) throws Exception {

        boolean includeStaff = SecurityUtils.canCurrentUserViewPrivateContent();
        NewsFilter filter = buildExportFilter(
                includeStaff, reporterIds, cameramanIds, locationIds, startDate, endDate,
                searchTerm, importerId, ingestorId, isPrivate
        );

        String username = (authentication != null) ? authentication.getName() : "SYSTEM";
        log.info("AUDIT_EXPORT_CSV | Module: NEWS | User: '{}' | IP: {} | Filter: {}",
                username, ClientIpUtil.getClientIp(httpRequest), filter);

        if (rejectIfTooLarge(filter, response)) {
            return;
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"news_export.csv\"");

        newsExportService.exportToCsv(filter, includeStaff, response.getWriter());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/news/export/pdf")
    public void exportPdf(
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
            HttpServletResponse response) throws Exception {

        boolean includeStaff = SecurityUtils.canCurrentUserViewPrivateContent();
        NewsFilter filter = buildExportFilter(
                includeStaff, reporterIds, cameramanIds, locationIds, startDate, endDate,
                searchTerm, importerId, ingestorId, isPrivate
        );

        String username = (authentication != null) ? authentication.getName() : "SYSTEM";
        log.info("AUDIT_EXPORT_PDF | Module: NEWS | User: '{}' | IP: {} | Filter: {}",
                username, ClientIpUtil.getClientIp(httpRequest), filter);

        if (rejectIfTooLarge(filter, response)) {
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"news_export.pdf\"");

        newsExportService.exportToPdf(filter, includeStaff, response.getOutputStream());
    }

    // Exports are capped so a huge result set cannot tie up the server (PDF especially).
    // Checked before any headers or bytes are written, so the client gets a clean JSON error.
    private boolean rejectIfTooLarge(NewsFilter filter, HttpServletResponse response) throws Exception {
        long total = newsExportService.countMatching(filter);
        if (total <= NewsExportService.MAX_EXPORT_ROWS) {
            return false;
        }
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"Too many results to export (" + total + "). The limit is "
                + NewsExportService.MAX_EXPORT_ROWS + " - narrow your filters and try again.\"}");
        return true;
    }

    // Viewers never get the staff filters or staff columns, and are pinned to public news.
    private NewsFilter buildExportFilter(
            boolean includeStaff,
            Set<Long> reporterIds,
            Set<Long> cameramanIds,
            Set<Long> locationIds,
            LocalDate startDate,
            LocalDate endDate,
            String searchTerm,
            Long importerId,
            Long ingestorId,
            Boolean isPrivate) {

        return new NewsFilter(
                reporterIds,
                cameramanIds,
                locationIds,
                startDate,
                endDate,
                searchTerm,
                includeStaff ? importerId : null,
                includeStaff ? ingestorId : null,
                includeStaff ? isPrivate : Boolean.FALSE
        );
    }
}