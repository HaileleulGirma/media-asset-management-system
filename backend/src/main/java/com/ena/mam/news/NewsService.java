package com.ena.mam.news;

import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.cameraman.CameramanRepository;
import com.ena.mam.dto.request.CreateNewsRequest;
import com.ena.mam.dto.request.filter.NewsFilter;
import com.ena.mam.dto.response.CreateNewsResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import com.ena.mam.location.Location;
import com.ena.mam.location.LocationRepository;
import com.ena.mam.reporter.Reporter;
import com.ena.mam.reporter.ReporterRepository;
import com.ena.mam.staffmember.StaffMember;
import com.ena.mam.staffmember.StaffMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public CreateNewsResponse create(CreateNewsRequest request) {

        Set<Cameraman> cameramen =
                new HashSet<>(cameramanRepository.findAllById(request.cameramanIds()));

        Set<Reporter> reporters =
                new HashSet<>(reporterRepository.findAllById(request.reporterIds()));

        Set<Location> locations =
                new HashSet<>(locationRepository.findAllById(request.locationIds()));

        StaffMember importer =
                staffMemberRepository.findById(request.importerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Importer not found: " + request.importerId()));

        StaffMember ingestor = null;
        if (request.ingestorId() != null) {
            ingestor = staffMemberRepository.findById(request.ingestorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingestor not found: " + request.ingestorId()));
        }

        News news = newsMapper.toNews(request, cameramen, reporters, importer, ingestor, locations);
        News savedNews = newsRepository.save(news);
        return newsMapper.toResponse(savedNews);
    }

    @Transactional
    public CreateNewsResponse update(Long newsId, CreateNewsRequest request) {
        if (request.version() == null) {
            throw new IllegalArgumentException("Version number is required when updating news.");
        }

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News with id %d not found.".formatted(newsId)));

        // Explicitly verify the client is updating the latest version
        if (!news.getVersion().equals(request.version())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(News.class, newsId);
        }

        Set<Cameraman> cameramen =
                new HashSet<>(cameramanRepository.findAllById(request.cameramanIds()));

        Set<Reporter> reporters =
                new HashSet<>(reporterRepository.findAllById(request.reporterIds()));

        Set<Location> locations =
                new HashSet<>(locationRepository.findAllById(request.locationIds()));

        StaffMember importer = staffMemberRepository.findById(request.importerId())
                .orElseThrow(() -> new ResourceNotFoundException("Importer with id %d not found.".formatted(request.importerId())));

        StaffMember ingestor = null;
        if (request.ingestorId() != null) {
            ingestor = staffMemberRepository.findById(request.ingestorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingestor with id %d not found.".formatted(request.ingestorId())));
        }

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

    @Transactional(readOnly = true)
    public Page<CreateNewsResponse> search(NewsFilter filter, Pageable pageable) {

        Specification<News> spec = Specification.allOf();

        // Loop through each reporter ID to enforce an "AND" condition
        if (filter.reporterIds() != null && !filter.reporterIds().isEmpty()) {
            for (Long reporterId : filter.reporterIds()) {
                spec = spec.and(NewsSpecification.hasReporter(reporterId));
            }
        }

        // Loop through each cameraman ID to enforce an "AND" condition
        if (filter.cameramanIds() != null && !filter.cameramanIds().isEmpty()) {
            for (Long cameramanId : filter.cameramanIds()) {
                spec = spec.and(NewsSpecification.hasCameraman(cameramanId));
            }
        }

        // Loop through each location ID to enforce an "AND" condition
        if (filter.locationIds() != null && !filter.locationIds().isEmpty()) {
            for (Long locationId : filter.locationIds()) {
                spec = spec.and(NewsSpecification.hasLocation(locationId));
            }
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
            spec = spec.and(NewsSpecification.hasImporterId(filter.importerId()));
        }

        if (filter.ingestorId() != null) {
            spec = spec.and(NewsSpecification.hasIngestorId(filter.ingestorId()));
        }

        if (filter.searchTerm() != null && !filter.searchTerm().isBlank()) {
            spec = spec.and(NewsSpecification.hasSearchTerm(filter.searchTerm()));
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("newsDate").descending()
        );

        return newsRepository
                .findAll(spec, sortedPageable)
                .map(newsMapper::toResponse);
    }
}