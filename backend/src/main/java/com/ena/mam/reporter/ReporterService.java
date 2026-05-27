package com.ena.mam.reporter;

import com.ena.mam.dto.request.CreateReporterRequest;
import com.ena.mam.dto.response.CreateReporterResponse;
import org.springframework.stereotype.Service;

@Service
public class ReporterService {
    private final ReporterMapper reporterMapper;
    private final ReporterRepository reporterRepository;

    public ReporterService(ReporterMapper reporterMapper, ReporterRepository reporterRepository) {
        this.reporterMapper = reporterMapper;
        this.reporterRepository = reporterRepository;
    }

    public CreateReporterResponse create(CreateReporterRequest request){
        Reporter reporter = reporterMapper.toReporter(request);
        Reporter savedReporter = reporterRepository.save(reporter);
        return reporterMapper.toResponse(savedReporter);
    }
}
