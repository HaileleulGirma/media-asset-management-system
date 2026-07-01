package com.ena.mam.news;

import com.ena.mam.dto.request.CreateNewsRequest;
import com.ena.mam.dto.request.NewsFilter;
import com.ena.mam.dto.response.CreateNewsResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@RestController
public class NewsController {
    private final NewsService newsService;


    public NewsController( NewsService newsService) {
        this.newsService = newsService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/api/news")
    public CreateNewsResponse create(@Valid @RequestBody CreateNewsRequest request){
        return newsService.create(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/api/news/{newsId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long newsId
    ){
        newsService.delete(newsId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/news")
    public Page<CreateNewsResponse> search(
            @RequestParam(required = false)
            Set<Long> reporterIds,

            @RequestParam(required = false)
            Set<Long> cameramanIds,

            @RequestParam(required = false)
            Set<Long> locationIds,

            @RequestParam(required = false)
            LocalDate startDate,

            @RequestParam(required = false)
            LocalDate endDate,

            @RequestParam(required = false)
            String searchTerm,

            @RequestParam(required = false)
            Long importerId,

            @RequestParam(required = false)
            Long ingestorId,

            Pageable pageable
    ) {

        NewsFilter filter =
                new NewsFilter(
                        reporterIds,
                        cameramanIds,
                        locationIds,
                        startDate,
                        endDate,
                        searchTerm,
                        importerId,
                        ingestorId
                );

        return newsService.search(
                filter,
                pageable
        );
    }
}
