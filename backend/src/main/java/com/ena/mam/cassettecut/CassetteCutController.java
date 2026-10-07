package com.ena.mam.cassettecut;

import com.ena.mam.dto.request.CreateCassetteCutRequest;
import com.ena.mam.dto.request.filter.CassetteCutFilter;
import com.ena.mam.dto.response.CreateCassetteCutResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class CassetteCutController {

    private final CassetteCutService cassetteCutService;

    public CassetteCutController(CassetteCutService cassetteCutService) {
        this.cassetteCutService = cassetteCutService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/api/cassette-cuts")
    public CreateCassetteCutResponse create(
            @Valid @RequestBody CreateCassetteCutRequest request
    ) {
        return cassetteCutService.create(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/api/cassette-cuts/{cutId}")
    public CreateCassetteCutResponse update(
            @PathVariable Long cutId,
            @Valid @RequestBody CreateCassetteCutRequest request
    ) {
        return cassetteCutService.update(cutId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/api/cassette-cuts/{cutId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long cutId
    ) {
        cassetteCutService.delete(cutId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'VIEWER')")
    @GetMapping("/api/cassette-cuts")
    public Page<CreateCassetteCutResponse> search(
            @RequestParam(required = false)
            Long digitizedMediaId,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            java.time.LocalDate cutDate,

            @RequestParam(required = false)
            String searchTerm,

            Pageable pageable
    ) {
        CassetteCutFilter filter =
                new CassetteCutFilter(
                        digitizedMediaId,
                        categoryId,
                        cutDate,
                        searchTerm
                );

        return cassetteCutService.search(
                filter,
                pageable
        );
    }
}