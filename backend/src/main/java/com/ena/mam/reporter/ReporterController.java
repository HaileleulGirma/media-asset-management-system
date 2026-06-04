package com.ena.mam.reporter;

import com.ena.mam.dto.request.CreateReporterRequest;
import com.ena.mam.dto.response.CreateReporterResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReporterController {

    private final ReporterService reporterService;

    public ReporterController(ReporterService reporterService) {
        this.reporterService = reporterService;
    }


    @PostMapping("/api/reporter/create")
    public CreateReporterResponse create(
            @Valid @RequestBody CreateReporterRequest request){
        return reporterService.create(request);
    }

    @PutMapping("/api/reporter/{reporterId}")
    public CreateReporterResponse update(
            @PathVariable Long reporterId,
            @Valid @RequestBody CreateReporterRequest request){
        return reporterService.update(reporterId, request);
    }

    @DeleteMapping("/api/reporter/{reporterId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long reporterId
    )
    {
        reporterService.delete(reporterId);

        return ResponseEntity.noContent().build();
    }
}
