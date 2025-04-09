package com.example.hacker_cnews.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HackerNewsConfig {
    
    @Value("${hacker-news.items.limit}")
    private int itemsLimit;
    
    @Value("${hacker-news.max-stored-items}")
    private int maxStoredItems;
    
    @Value("${hacker-news.poll.interval}")
    private long pollInterval;
    
    public int getItemsLimit() {
        return itemsLimit;
    }
    
    public int getMaxStoredItems() {
        return maxStoredItems;
    }
    
    public long getPollInterval() {
        return pollInterval;
    }
} 