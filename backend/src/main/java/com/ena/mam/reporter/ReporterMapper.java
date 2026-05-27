package com.ena.mam.reporter;

import com.ena.mam.dto.request.CreateReporterRequest;
import com.ena.mam.dto.response.CreateReporterResponse;
import org.springframework.stereotype.Component;

@Component
public class ReporterMapper {
    public Reporter toReporter(CreateReporterRequest request){
        Reporter reporter = new Reporter();
        reporter.setReporterName(request.reporterName());
        reporter.setActive(request.isActive());

        return reporter;
    }

    public CreateReporterResponse toResponse(Reporter reporter){
        return new CreateReporterResponse(
                reporter.getReporterId(),
                reporter.getReporterName(),
                reporter.getActive()
                );
    }
}
