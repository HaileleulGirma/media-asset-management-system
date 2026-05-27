package com.ena.mam.news;

import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.reporter.Reporter;
import com.ena.mam.staffmember.StaffMember;
import com.ena.mam.dto.request.CreateNewsRequest;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class NewsMapper {

    public News toNews(CreateNewsRequest request,
                       Set<Cameraman> cameramen,
                       Set<Reporter> reporters,
                       Set<StaffMember> importers,
                       Set<StaffMember> ingestors) {

        News news = new News();
        news.setTitle(request.title());
        news.setNewsDate(request.newsDate());
        news.setCameramen(cameramen);
        news.setReporters(reporters);
        news.setImporters(importers);
        news.setIngestors(ingestors);
        news.setLocation(request.location());
        news.setNumberOfFiles(request.numberOfFiles());
        news.setTotalSize(request.totalSize());

        return news;
    }
}