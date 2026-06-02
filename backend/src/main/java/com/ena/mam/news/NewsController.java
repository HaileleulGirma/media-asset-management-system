package com.ena.mam.news;

import com.ena.mam.dto.request.CreateNewsRequest;
import com.ena.mam.dto.response.CreateNewsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NewsController {
    private final NewsService newsService;


    public NewsController( NewsService newsService) {
        this.newsService = newsService;
    }

    @PostMapping("api/create/news")
    public CreateNewsResponse create(@Valid @RequestBody CreateNewsRequest request){
        return newsService.create(request);
    }


}
