package com.example.hacker_cnews.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hacker_cnews.entity.NewsItem;
import com.example.hacker_cnews.repository.NewsItemRepository;
import com.example.hacker_cnews.service.TranslationService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsItemRepository repository;
    private final TranslationService translationService;
    
    public NewsController(NewsItemRepository repository, TranslationService translationService) {
        this.repository = repository;
        this.translationService = translationService;
    }
    
    @GetMapping
    public List<NewsItem> getLatestNews() {
        return repository.findTop50ByOrderByTimeDesc();
    }
    
    @GetMapping("/{id}")
    public Optional<NewsItem> getNewsById(@PathVariable Long id) {
        return repository.findById(id);
    }
    
    @GetMapping("/test-translate")
    public Mono<String> testTranslate(@RequestParam String text) {
        return translationService.translateEnToZh(text);
    }
} 