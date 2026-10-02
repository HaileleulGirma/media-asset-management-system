package com.ena.mam.photo;

import com.ena.mam.dto.request.CreatePhotoRequest;
import com.ena.mam.dto.request.filter.PhotoFilter;
import com.ena.mam.dto.response.CreatePhotoResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/api/photos")
    public CreatePhotoResponse create(
            @Valid @RequestBody CreatePhotoRequest request
    ) {
        return photoService.create(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/api/photos/{photoId}")
    public CreatePhotoResponse update(
            @PathVariable Long photoId,
            @Valid @RequestBody CreatePhotoRequest request
    ) {
        return photoService.update(photoId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/api/photos/{photoId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long photoId
    ) {
        photoService.delete(photoId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/photos")
    public Page<CreatePhotoResponse> search(
            @RequestParam(required = false)
            LocalDate startDate,

            @RequestParam(required = false)
            LocalDate endDate,

            @RequestParam(required = false)
            String searchTerm,

            @RequestParam(required = false)
            Long importerId,

            Pageable pageable
    ) {

        PhotoFilter filter =
                new PhotoFilter(
                        startDate,
                        endDate,
                        searchTerm,
                        importerId
                );

        return photoService.search(
                filter,
                pageable
        );
    }
}