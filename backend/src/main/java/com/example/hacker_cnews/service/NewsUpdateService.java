package com.example.hacker_cnews.service;

import com.example.hacker_cnews.entity.NewsItem;
import com.example.hacker_cnews.repository.NewsItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class NewsUpdateService {
    private final HackerNewsService hackerNewsService;
    private final TranslationService translationService;
    private final NewsItemRepository repository;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    
    @Value("${hacker-news.items.limit}")
    private int itemsLimit;
    
    public NewsUpdateService(
            HackerNewsService hackerNewsService,
            TranslationService translationService,
            NewsItemRepository repository,
            CacheService cacheService,
            ObjectMapper objectMapper) {
        this.hackerNewsService = hackerNewsService;
        this.translationService = translationService;
        this.repository = repository;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }
    
    @Scheduled(fixedDelayString = "${hacker-news.poll.interval}")
    public void updateNews() {
        hackerNewsService.getTopStories(itemsLimit)
                .flatMapIterable(ids -> ids)
                .flatMap(id -> {
                    Object cached = cacheService.getCachedNewsItem(id);
                    if (cached != null && cached instanceof NewsItem) {
                        return Mono.just((NewsItem) cached);
                    }
                    
                    return processNewsItem(id);
                })
                .collectList()
                .subscribe(items -> {
                    repository.saveAll(items);
                    System.out.println("已保存 " + items.size() + " 条新闻");
                });
    }
    
    private Mono<NewsItem> processNewsItem(Long id) {
        return hackerNewsService.getItemById(id)
                .flatMap(hnItem -> {
                    // 检查数据库是否已存在
                    if (repository.existsById(id)) {
                        return repository.findById(id).map(Mono::just).orElseGet(Mono::empty);
                    }
                    
                    NewsItem newsItem = new NewsItem();
                    newsItem.setId(hnItem.getId());
                    newsItem.setTitleEn(hnItem.getTitle());
                    newsItem.setUrl(hnItem.getUrl());
                    newsItem.setTextEn(hnItem.getText());
                    newsItem.setType(hnItem.getType());
                    newsItem.setTime(hnItem.getTime());
                    newsItem.setCreatedAt(Instant.now());
                    
                    try {
                        if (hnItem.getKids() != null) {
                            newsItem.setCommentIds(objectMapper.writeValueAsString(hnItem.getKids()));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    // 查找缓存中是否有翻译
                    String cachedTitle = cacheService.getCachedTranslation(hnItem.getTitle());
                    String cachedText = hnItem.getText() != null ? 
                            cacheService.getCachedTranslation(hnItem.getText()) : null;
                    
                    Mono<String> titleTranslation = cachedTitle != null ? 
                            Mono.just(cachedTitle) : 
                            translationService.translateEnToZh(hnItem.getTitle());
                    
                    Mono<String> textTranslation = hnItem.getText() == null ? Mono.just("") :
                            (cachedText != null ? Mono.just(cachedText) : 
                            translationService.translateEnToZh(hnItem.getText()));
                    
                    return Mono.zip(titleTranslation, textTranslation)
                            .map(tuple -> {
                                String translatedTitle = tuple.getT1();
                                String translatedText = tuple.getT2();
                                
                                // 保存到缓存
                                if (cachedTitle == null) {
                                    cacheService.cacheTranslation(hnItem.getTitle(), translatedTitle);
                                }
                                
                                if (cachedText == null && hnItem.getText() != null) {
                                    cacheService.cacheTranslation(hnItem.getText(), translatedText);
                                }
                                
                                newsItem.setTitleZh(translatedTitle);
                                newsItem.setTextZh(translatedText);
                                
                                // 缓存新闻项
                                cacheService.cacheNewsItem(newsItem.getId(), newsItem);
                                
                                return newsItem;
                            });
                });
    }
} 