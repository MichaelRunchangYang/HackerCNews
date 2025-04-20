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
import com.example.hacker_cnews.service.CacheService;

import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsItemRepository repository;
    private final TranslationService translationService;
    private final CacheService cacheService;
    private static final Logger logger = LoggerFactory.getLogger(NewsController.class);
    
    public NewsController(NewsItemRepository repository, TranslationService translationService, CacheService cacheService) {
        this.repository = repository;
        this.translationService = translationService;
        this.cacheService = cacheService;
    }
    
    @GetMapping
    public List<NewsItem> getLatestNews() {
        logger.info("请求获取最新新闻列表");
        List<NewsItem> newsList = repository.findTop50ByOrderByTimeDesc();

        if (newsList != null && !newsList.isEmpty()) {
            logger.debug("尝试为获取到的 {} 条新闻填充缓存...", newsList.size());
            newsList.forEach(item -> {
                try {
                    NewsItem cachedItem = cacheService.getCachedNewsItem(item.getId());
                    if (cachedItem == null) {
                        logger.debug("缓存未命中，填充新闻项: {}", item.getId());
                        cacheService.cacheNewsItem(item.getId(), item);
                    } else {
                        // 可选：如果需要，可以在这里比较DB和缓存版本，并更新缓存
                        // logger.debug("缓存命中: {}", item.getId());
                    }
                } catch (Exception e) {
                    logger.error("填充缓存时出错，新闻ID: {}, 错误: {}", item.getId(), e.getMessage());
                }
            });
        }
        logger.info("返回 {} 条最新新闻", newsList != null ? newsList.size() : 0);
        return newsList;
    }
    
    @GetMapping("/{id}")
    public Optional<NewsItem> getNewsById(@PathVariable Long id) {
        logger.info("请求获取新闻详情，ID: {}", id);
        NewsItem cachedItem = null;
        try {
            cachedItem = cacheService.getCachedNewsItem(id);
        } catch (Exception e) {
            logger.error("从缓存获取新闻项时出错 ID: {}, 错误: {}", id, e.getMessage());
        }

        if (cachedItem != null) {
            logger.info("缓存命中，返回缓存中的新闻项: {}", id);
            return Optional.of(cachedItem);
        }

        logger.info("缓存未命中，从数据库查询新闻项: {}", id);
        Optional<NewsItem> dbItemOptional = repository.findById(id);

        if (dbItemOptional.isPresent()) {
            logger.info("数据库查询命中，将结果写入缓存: {}", id);
            try {
                cacheService.cacheNewsItem(id, dbItemOptional.get());
            } catch (Exception e) {
                logger.error("写入缓存时出错 ID: {}, 错误: {}", id, e.getMessage());
            }
        } else {
            logger.info("数据库查询未命中: {}", id);
            // 可选：对于未找到的ID，可以缓存一个特殊的"空"标记，防止缓存穿透
            // cacheService.cacheEmptyNewsItem(id); // 需要在CacheService中实现
        }

        return dbItemOptional;
    }
    
    @GetMapping("/test-translate")
    public Mono<String> testTranslate(@RequestParam String text) {
        return translationService.translateEnToZh(text);
    }
} 