package com.ena.mam;

import com.ena.mam.dto.request.CreateNewsRequest;
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

    public NewsService(CameramanRepository cameramanRepository, ReporterRepository reporterRepository, NewsRepository newsRepository, NewsMapper newsMapper, StaffMemberRepository staffMemberRepository) {
        this.cameramanRepository = cameramanRepository;
        this.reporterRepository = reporterRepository;
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
        this.staffMemberRepository = staffMemberRepository;
    }

    public News create(CreateNewsRequest request){
        Set<Cameraman> cameramen = new HashSet<>(cameramanRepository.findAllById(request.cameramanIds()));
        Set<Reporter> reporters = new HashSet<>(reporterRepository.findAllById(request.reporterIds()));
        Set<StaffMember> importers = new HashSet<>(staffMemberRepository.findAllById(request.importerIds()));
        Set<StaffMember> ingestors = new HashSet<>(staffMemberRepository.findAllById(request.ingestorIds()));

        News news = newsMapper.toNews(request, cameramen, reporters, importers, ingestors);

        return newsRepository.save(news);
    }



}
