package com.ena.mam.news;

import com.ena.mam.dto.request.CreateNewsRequest;
import com.ena.mam.dto.response.CreateNewsResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class NewsController {
    private final NewsService newsService;


    public NewsController( NewsService newsService) {
        this.newsService = newsService;
    }

    @PostMapping("api/news")
    public CreateNewsResponse create(@Valid @RequestBody CreateNewsRequest request){
        return newsService.create(request);
    }

    @DeleteMapping("/api/news/{newsId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long newsId
    ){
        newsService.delete(newsId);

        return ResponseEntity.noContent().build();
    }
}
