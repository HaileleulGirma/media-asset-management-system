package com.ena.mam.news;

import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.cameraman.CameramanRepository;
import com.ena.mam.dto.request.CreateNewsRequest;
import com.ena.mam.dto.request.NewsFilter;
import com.ena.mam.dto.response.CreateNewsResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import com.ena.mam.location.Location;
import com.ena.mam.location.LocationRepository;
import com.ena.mam.reporter.Reporter;
import com.ena.mam.reporter.ReporterRepository;
import com.ena.mam.staffmember.StaffMember;
import com.ena.mam.staffmember.StaffMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class NewsService {

    private final CameramanRepository cameramanRepository;
    private final ReporterRepository reporterRepository;
    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final StaffMemberRepository staffMemberRepository;
    private final LocationRepository locationRepository;

    public NewsService(
            CameramanRepository cameramanRepository,
            ReporterRepository reporterRepository,
            NewsRepository newsRepository,
            NewsMapper newsMapper,
            StaffMemberRepository staffMemberRepository,
            LocationRepository locationRepository
    ) {
        this.cameramanRepository = cameramanRepository;
        this.reporterRepository = reporterRepository;
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
        this.staffMemberRepository = staffMemberRepository;
        this.locationRepository = locationRepository;
    }

    public CreateNewsResponse create(CreateNewsRequest request) {

        Set<Cameraman> cameramen =
                new HashSet<>(cameramanRepository.findAllById(request.cameramanIds()));

        Set<Reporter> reporters =
                new HashSet<>(reporterRepository.findAllById(request.reporterIds()));

        Set<Location> locations =
                new HashSet<>(locationRepository.findAllById(request.locationIds()));

        StaffMember importer =
                staffMemberRepository.findById(request.importerId())
                        .orElseThrow(() -> new RuntimeException("Importer not found"));

        StaffMember ingestor =
                staffMemberRepository.findById(request.ingestorId())
                        .orElseThrow(() -> new RuntimeException("Ingestor not found"));

        News news = newsMapper.toNews(
                request,
                cameramen,
                reporters,
                importer,
                ingestor,
                locations
        );

        News savedNews = newsRepository.save(news);

        return newsMapper.toResponse(savedNews);
    }

    public CreateNewsResponse update(Long newsId, CreateNewsRequest request){
        News news = newsRepository.findById(newsId).orElseThrow(() -> new ResourceNotFoundException("News with id %d not found.".formatted(newsId)));

        Set<Cameraman> cameramen =
                new HashSet<>(cameramanRepository.findAllById(request.cameramanIds()));

        Set<Reporter> reporters =
                new HashSet<>(reporterRepository.findAllById(request.reporterIds()));

        StaffMember importer = staffMemberRepository.findById(request.importerId()).orElseThrow(() -> new ResourceNotFoundException("Staff member with id %d not found.".formatted(request.importerId())));
        StaffMember ingestor = staffMemberRepository.findById(request.ingestorId()).orElseThrow(() -> new ResourceNotFoundException("Staff member with id %d not found.".formatted(request.ingestorId())));

        Set<Location> locations =
                new HashSet<>(locationRepository.findAllById(request.locationIds()));

        newsMapper.updateNews(
                news,
                request,
                cameramen,
                reporters,
                importer,
                ingestor,
                locations
        );

        News savedNews = newsRepository.save(news);

        return newsMapper.toResponse(savedNews);
    }

    public void delete(Long newsId){
        newsRepository.deleteById(newsId);
    }

    public Page<CreateNewsResponse> search(
            NewsFilter filter,
            Pageable pageable) {

        Specification<News> spec =
                Specification.allOf();

        if (filter.reporterIds() != null &&
                !filter.reporterIds().isEmpty()) {

            spec = spec.and(
                    NewsSpecification.hasReporters(
                            filter.reporterIds()));
        }

        if (filter.cameramanIds() != null &&
                !filter.cameramanIds().isEmpty()) {

            spec = spec.and(
                    NewsSpecification.hasCameramen(
                            filter.cameramanIds()));
        }

        if (filter.locationIds() != null &&
                !filter.locationIds().isEmpty()) {

            spec = spec.and(
                    NewsSpecification.hasLocations(
                            filter.locationIds()));
        }

        if (filter.startDate() != null) {
            spec = spec.and(
                    NewsSpecification.hasNewsDateBetween(
                            filter.startDate(),
                            filter.effectiveEndDate()
                    )
            );
        }

        if (filter.importerId() != null) {
            spec = spec.and(
                    NewsSpecification.hasImporterId(filter.importerId()));
        }

        if (filter.importerId() != null) {
            spec = spec.and(
                    NewsSpecification.hasIngestorId(filter.ingestorId()));
        }

        return newsRepository
                .findAll(spec, pageable)
                .map(newsMapper::toResponse);
    }
}