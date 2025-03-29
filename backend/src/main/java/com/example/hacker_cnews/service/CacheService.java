package com.example.hacker_cnews.service;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    
    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public void cacheTranslation(String key, String translation) {
        redisTemplate.opsForValue().set("translate:" + key, translation, 24, TimeUnit.HOURS);
    }
    
    public String getCachedTranslation(String key) {
        Object value = redisTemplate.opsForValue().get("translate:" + key);
        return value != null ? value.toString() : null;
    }
    
    public void cacheNewsItem(Long id, Object newsItem) {
        redisTemplate.opsForValue().set("news:" + id, newsItem, 1, TimeUnit.HOURS);
    }
    
    public Object getCachedNewsItem(Long id) {
        return redisTemplate.opsForValue().get("news:" + id);
    }
} 