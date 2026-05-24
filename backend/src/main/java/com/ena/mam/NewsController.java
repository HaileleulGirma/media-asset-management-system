package com.ena.mam;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class NewsController {
    private final NewsRepository newsRepository;

    public NewsController(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

}
