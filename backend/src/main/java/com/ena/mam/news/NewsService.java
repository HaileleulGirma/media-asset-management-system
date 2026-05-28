package com.ena.mam.news;

import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.cameraman.CameramanRepository;
import com.ena.mam.dto.request.CreateNewsRequest;
import com.ena.mam.dto.response.CreateNewsResponse;
import com.ena.mam.location.Location;
import com.ena.mam.location.LocationRepository;
import com.ena.mam.reporter.Reporter;
import com.ena.mam.reporter.ReporterRepository;
import com.ena.mam.staffmember.StaffMember;
import com.ena.mam.staffmember.StaffMemberRepository;
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
}