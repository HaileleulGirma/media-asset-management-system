package com.ena.mam.reporter;

import com.ena.mam.dto.request.CreateReporterRequest;
import com.ena.mam.dto.response.CreateReporterResponse;
import com.ena.mam.exception.ResourceNotFoundException;
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

    public CreateReporterResponse update(Long id, CreateReporterRequest request){
        Reporter reporter = reporterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter with id %d not found.".formatted(id)));

        reporter.setReporterName(request.reporterName());
        reporter.setActive(request.isActive());

        Reporter savedReporter = reporterRepository.save(reporter);

        return reporterMapper.toResponse(savedReporter);
    }

    public void delete(Long id){
        reporterRepository.deleteById(id);
    }
}
