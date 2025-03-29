package com.example.hacker_cnews.service;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class HackerNewsService {
    private final WebClient webClient;
    private static final String HN_API_BASE_URL = "https://hacker-news.firebaseio.com/v0";
    
    public HackerNewsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(HN_API_BASE_URL).build();
    }
    
    public Mono<List<Long>> getTopStories(int limit) {
        return webClient.get()
                .uri("/topstories.json")
                .retrieve()
                .bodyToMono(Long[].class)
                .map(array -> Arrays.asList(array).subList(0, Math.min(array.length, limit)));
    }
    
    public Mono<HackerNewsItem> getItemById(Long id) {
        return webClient.get()
                .uri("/item/{id}.json", id)
                .retrieve()
                .bodyToMono(HackerNewsItem.class);
    }
    
    // 内部类表示HN API返回的数据结构
    public static class HackerNewsItem {
        private Long id;
        private String title;
        private String url;
        private String text;
        private String type;
        private Long time;
        private List<Long> kids;
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Long getTime() {
            return time;
        }
        
        public void setTime(Long time) {
            this.time = time;
        }
        
        public List<Long> getKids() {
            return kids;
        }
        
        public void setKids(List<Long> kids) {
            this.kids = kids;
        }
    }
} 