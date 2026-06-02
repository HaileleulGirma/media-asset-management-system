package com.ena.mam.reporter;

import com.ena.mam.dto.request.CreateReporterRequest;
import com.ena.mam.dto.response.CreateReporterResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
