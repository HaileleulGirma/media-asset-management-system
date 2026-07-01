package com.ena.mam.reporter;

import com.ena.mam.dto.request.CreateReporterRequest;
import com.ena.mam.dto.response.CreateReporterResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReporterController {

    private final ReporterService reporterService;

    public ReporterController(ReporterService reporterService) {
        this.reporterService = reporterService;
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/api/reporter/create")
    public CreateReporterResponse create(
            @Valid @RequestBody CreateReporterRequest request){
        return reporterService.create(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/reporter/{reporterId}")
    public CreateReporterResponse update(
            @PathVariable Long reporterId,
            @Valid @RequestBody CreateReporterRequest request){
        return reporterService.update(reporterId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/reporter/{reporterId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long reporterId
    )
    {
        reporterService.delete(reporterId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/reporter/{reporterId}")
    public CreateReporterResponse findUsingId(
            @PathVariable Long reporterId
    ){
       return reporterService.findReporter(reporterId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/reporter")
    public List<CreateReporterResponse> findAll() {
        return reporterService.findAll();
    }
}
