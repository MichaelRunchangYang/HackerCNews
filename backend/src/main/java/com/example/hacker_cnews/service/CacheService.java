package com.example.hacker_cnews.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.hacker_cnews.entity.NewsItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private final RedisTemplate<String, NewsItem> newsItemRedisTemplate;
    
    // 缓存有效期设置 (24小时)
    private static final long CACHE_TTL = 24 * 60 * 60;
    
    // 缓存前缀
    private static final String TRANSLATION_PREFIX = "translation:";
    private static final String NEWS_PREFIX = "news:";
    
    /**
     * 缓存翻译结果
     * 
     * @param key 翻译的原文
     * @param translation 翻译的结果
     */
    public void cacheTranslation(String key, String translation) {
        String cacheKey = TRANSLATION_PREFIX + key;
        redisTemplate.opsForValue().set(cacheKey, translation, CACHE_TTL, TimeUnit.SECONDS);
    }
    
    /**
     * 获取缓存的翻译结果
     * 
     * @param key 翻译的原文
     * @return 缓存的翻译结果，如果没有则返回null
     */
    public String getCachedTranslation(String key) {
        String cacheKey = TRANSLATION_PREFIX + key;
        return redisTemplate.opsForValue().get(cacheKey);
    }
    
    /**
     * 缓存新闻项
     * 
     * @param id 新闻ID
     * @param newsItem 新闻项对象
     */
    public void cacheNewsItem(Long id, NewsItem newsItem) {
        String cacheKey = NEWS_PREFIX + id;
        newsItemRedisTemplate.opsForValue().set(cacheKey, newsItem, CACHE_TTL, TimeUnit.SECONDS);
    }
    
    /**
     * 获取缓存的新闻项
     * 
     * @param id 新闻ID
     * @return 缓存的新闻项对象，如果没有则返回null
     */
    public NewsItem getCachedNewsItem(Long id) {
        String cacheKey = NEWS_PREFIX + id;
        return newsItemRedisTemplate.opsForValue().get(cacheKey);
    }
} 