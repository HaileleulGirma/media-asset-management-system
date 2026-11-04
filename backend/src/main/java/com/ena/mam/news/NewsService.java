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
import com.ena.mam.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

@Service
public class NewsService {

    // Staff may modify/delete news whose newsDate is within this many days of today.
    // Older items can only be modified or deleted by an admin.
    private static final int STAFF_EDIT_WINDOW_DAYS = 7;
    private static final ZoneId ADDIS = ZoneId.of("Africa/Addis_Ababa");

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

    // ---------- Edit-window gatekeeper ----------

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private boolean withinStaffWindow(LocalDate date) {
        if (date == null) return false;
        LocalDate cutoff = LocalDate.now(ADDIS).minusDays(STAFF_EDIT_WINDOW_DAYS);
        return !date.isBefore(cutoff);
    }

    private void assertCanModify(News news) {
        if (isAdmin()) return;
        if (!withinStaffWindow(news.getNewsDate())) {
            throw new AccessDeniedException(
                    "Only an admin can modify news older than " + STAFF_EDIT_WINDOW_DAYS + " days.");
        }
    }

    // ---------- Create ----------

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

    // ---------- Update ----------

    @Transactional
    public CreateNewsResponse update(Long newsId, CreateNewsRequest request) {
        if (request.version() == null) {
            throw new IllegalArgumentException("Version number is required when updating news.");
        }

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News with id %d not found.".formatted(newsId)));

        // Gatekeeper: checked against the STORED date, before anything is mapped.
        assertCanModify(news);

        // Staff also cannot move an item to a date outside their own window.
        if (!isAdmin() && !withinStaffWindow(request.newsDate())) {
            throw new AccessDeniedException(
                    "Only an admin can set a news date older than " + STAFF_EDIT_WINDOW_DAYS + " days.");
        }

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

    // ---------- Delete ----------

    @Transactional
    public void delete(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News with id %d not found.".formatted(newsId)));
        assertCanModify(news);
        newsRepository.delete(news);
    }

    // ---------- Search ----------

    @Transactional(readOnly = true)
    public Page<CreateNewsResponse> search(NewsFilter filter, Pageable pageable) {

        Specification<News> spec = Specification.allOf();
        boolean canViewPrivate = SecurityUtils.canCurrentUserViewPrivateContent();

        // Viewers cannot search private news.
        if (!canViewPrivate) {
            spec = spec.and(NewsSpecification.isPublic());
        } else if (filter.isPrivate() != null) {
            // Staff and admins can request either privacy status.
            spec = spec.and(
                    NewsSpecification.hasPrivacyStatus(filter.isPrivate())
            );
        }

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