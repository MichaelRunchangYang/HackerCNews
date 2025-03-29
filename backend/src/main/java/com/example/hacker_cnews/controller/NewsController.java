package com.example.hacker_cnews.controller;

import com.example.hacker_cnews.entity.NewsItem;
import com.example.hacker_cnews.repository.NewsItemRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsItemRepository repository;
    
    public NewsController(NewsItemRepository repository) {
        this.repository = repository;
    }
    
    @GetMapping
    public List<NewsItem> getLatestNews() {
        return repository.findTop30ByOrderByTimeDesc();
    }
    
    @GetMapping("/{id}")
    public Optional<NewsItem> getNewsById(@PathVariable Long id) {
        return repository.findById(id);
    }
} 