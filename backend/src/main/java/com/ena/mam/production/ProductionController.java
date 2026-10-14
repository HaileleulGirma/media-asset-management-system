package com.ena.mam.production;

import com.ena.mam.dto.request.CreateProductionRequest;
import com.ena.mam.dto.request.filter.ProductionFilter;
import com.ena.mam.dto.response.CreateProductionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class ProductionController {
    private final ProductionService service;

    public ProductionController(ProductionService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/production")
    public CreateProductionResponse create(
            @RequestParam CreateProductionRequest request){
        return service.create(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/production")
    public CreateProductionResponse update(
            @RequestParam Long id,
            @RequestParam CreateProductionRequest request){
        return service.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/production/{productionId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long productionId
    ){
        service.delete(productionId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/production")
    public Page<CreateProductionResponse> search(
            @RequestParam(required = false)
            String searchTerm,

            @RequestParam(required = false)
            LocalDate startDate,

            @RequestParam(required = false)
            LocalDate endDate,
            Pageable pageable
    ){
        ProductionFilter filter = new ProductionFilter(searchTerm, startDate, endDate);

        return service.search(filter, pageable);
    }
}
