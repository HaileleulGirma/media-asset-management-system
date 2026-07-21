package com.ena.mam.news;

import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.dto.request.CreateNewsRequest;
import com.ena.mam.dto.response.CreateNewsResponse;
import com.ena.mam.location.Location;
import com.ena.mam.reporter.Reporter;
import com.ena.mam.staffmember.StaffMember;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class NewsMapper {

    public News toNews(CreateNewsRequest request,
                       Set<Cameraman> cameramen,
                       Set<Reporter> reporters,
                       StaffMember importer,
                       StaffMember ingestor,
                       Set<Location> location) {

        News news = new News();
        news.setTitle(request.title());
        news.setNewsDate(request.newsDate());
        news.setCameramen(cameramen);
        news.setReporters(reporters);
        news.setFilePath(request.filePath());
        news.setImporter(importer);
        news.setIngestor(ingestor);
        news.setLocations(location);
        news.setNumberOfFiles(request.numberOfFiles());
        news.setTotalSize(request.totalSize());

        return news;
    }

    public CreateNewsResponse toResponse(News news) {

        return new CreateNewsResponse(
                news.getNewsId(),
                news.getNewsDate(),

                news.getCameramen()
                        .stream()
                        .map(Cameraman::getCameramanId)
                        .collect(Collectors.toSet()),

                news.getReporters()
                        .stream()
                        .map(Reporter::getReporterId)
                        .collect(Collectors.toSet()),

                news.getTitle(),

                news.getLocations()
                        .stream()
                        .map(Location::getLocationId)
                        .collect(Collectors.toSet()),

                news.getFilePath(),

                news.getImporter().getStaffMemberId(),

                news.getIngestor().getStaffMemberId(),

                news.getNumberOfFiles(),
                news.getTotalSize()
        );
    }

    public void updateNews(
            News news,
            CreateNewsRequest request,
            Set<Cameraman> cameramen,
            Set<Reporter> reporters,
            StaffMember importer,
            StaffMember ingestor,
            Set<Location> locations) {

        news.setTitle(request.title());
        news.setCameramen(cameramen);
        news.setReporters(reporters);
        news.setImporter(importer);
        news.setIngestor(ingestor);
        news.setLocations(locations);
    }

}