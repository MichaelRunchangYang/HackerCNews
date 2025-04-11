package com.example.hacker_cnews.service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.hacker_cnews.entity.NewsItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
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
    
    /**
     * 清除所有缓存
     * 包括翻译缓存和新闻项缓存
     * 
     * @return 清除的键数量
     */
    public int clearAllCache() {
        int count = 0;
        
        try {
            // 清除翻译缓存
            Set<String> translationKeys = redisTemplate.keys(TRANSLATION_PREFIX + "*");
            if (translationKeys != null && !translationKeys.isEmpty()) {
                redisTemplate.delete(translationKeys);
                count += translationKeys.size();
                logger.info("已清除{}个翻译缓存", translationKeys.size());
            }
            
            // 清除新闻项缓存
            Set<String> newsKeys = redisTemplate.keys(NEWS_PREFIX + "*");
            if (newsKeys != null && !newsKeys.isEmpty()) {
                redisTemplate.delete(newsKeys);
                count += newsKeys.size();
                logger.info("已清除{}个新闻缓存", newsKeys.size());
            }
            
            logger.info("缓存清理完成，共清除{}个键", count);
            return count;
        } catch (Exception e) {
            logger.error("清除缓存时发生错误: {}", e.getMessage(), e);
            throw e;
        }
    }
} 