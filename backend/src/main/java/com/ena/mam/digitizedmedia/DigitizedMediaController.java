package com.ena.mam.digitizedmedia;

import com.ena.mam.dto.request.CreateDigitizedMediaRequest;
import com.ena.mam.dto.request.DigitizedMediaFilter;
import com.ena.mam.dto.response.CreateDigitizedMediaResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class DigitizedMediaController {

    private final DigitizedMediaService digitizedMediaService;

    public DigitizedMediaController(DigitizedMediaService digitizedMediaService) {
        this.digitizedMediaService = digitizedMediaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/api/digitized-media")
    public CreateDigitizedMediaResponse create(
            @Valid @RequestBody CreateDigitizedMediaRequest request
    ) {
        return digitizedMediaService.create(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/api/digitized-media/{digitizedMediaId}")
    public CreateDigitizedMediaResponse update(
            @PathVariable Long digitizedMediaId,
            @Valid @RequestBody CreateDigitizedMediaRequest request
    ) {
        return digitizedMediaService.update(digitizedMediaId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/api/digitized-media/{digitizedMediaId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long digitizedMediaId
    ) {
        digitizedMediaService.delete(digitizedMediaId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/digitized-media")
    public Page<CreateDigitizedMediaResponse> search(
            @RequestParam(required = false)
            Long importerId,

            @RequestParam(required = false)
            String searchTerm,

            Pageable pageable
    ) {
        DigitizedMediaFilter filter =
                new DigitizedMediaFilter(
                        importerId,
                        searchTerm
                );

        return digitizedMediaService.search(
                filter,
                pageable
        );
    }
}