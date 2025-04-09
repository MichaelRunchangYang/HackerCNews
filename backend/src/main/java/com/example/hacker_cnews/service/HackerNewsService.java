package com.example.hacker_cnews.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Service
public class HackerNewsService {
    private static final Logger logger = LoggerFactory.getLogger(HackerNewsService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String HN_API_BASE_URL = "https://hacker-news.firebaseio.com/v0";
    
    public HackerNewsService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        // 配置ObjectMapper以处理未知属性和空值
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // 配置HttpClient用于优化连接设置 - 增加超时时间
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000) // 60秒连接超时
            .responseTimeout(Duration.ofSeconds(120)) // 120秒响应超时
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS)) // 120秒读取超时
                .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS))); // 120秒写入超时
        
        // 创建请求记录器
        ExchangeFilterFunction logRequest = ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.debug("请求: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
        
        // 创建响应记录器
        ExchangeFilterFunction logResponse = ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            logger.debug("响应状态: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
        
        this.webClient = webClientBuilder
                .baseUrl(HN_API_BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest)
                .filter(logResponse)
                .build();
        
        logger.info("HackerNewsService 初始化完成，使用基本URL: {}", HN_API_BASE_URL);
    }
    
    public Mono<List<Long>> getTopStories(int limit) {
        logger.info("获取热门故事列表，限制: {}", limit);
        return webClient.get()
                .uri("/topstories.json")
                .retrieve()
                .bodyToMono(Long[].class)
                .map(array -> {
                    if (array == null || array.length == 0) {
                        logger.warn("API返回了空的故事ID列表");
                        return Collections.<Long>emptyList();
                    }
                    logger.info("获取到 {} 个故事ID", array.length);
                    return Arrays.asList(array).subList(0, Math.min(array.length, limit));
                })
                .timeout(Duration.ofSeconds(120))  // 增加超时时间到120秒
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))  // 使用指数退避策略，减少重试次数但增加间隔
                        .maxBackoff(Duration.ofSeconds(20))
                        .doBeforeRetry(retrySignal -> 
                            logger.info("重试获取热门故事 (第{}次)", retrySignal.totalRetries() + 1)))
                .doOnError(e -> logger.error("获取热门故事失败: {} - {}", e.getClass().getName(), e.getMessage()))
                .onErrorReturn(Collections.emptyList());
    }
    
    public Mono<HackerNewsItem> getItemById(Long id) {
        logger.info("获取新闻项详情，ID: {}", id);
        
        // 首先尝试获取原始JSON数据
        return webClient.get()
                .uri("/item/{id}.json", id)
                .retrieve()
                .bodyToMono(String.class)  // 获取原始JSON
                .doOnNext(rawJson -> {
                    logger.debug("获取到原始JSON: {}", rawJson);
                })
                .flatMap(rawJson -> {
                    if (rawJson == null || rawJson.equals("null") || rawJson.trim().isEmpty()) {
                        logger.error("API返回了null或空JSON，ID: {}", id);
                        HackerNewsItem emptyItem = new HackerNewsItem();
                        emptyItem.setId(id);
                        return Mono.just(emptyItem);
                    }
                    
                    try {
                        // 手动解析JSON，并打印详细信息用于调试
                        logger.info("准备解析JSON，原始内容: {}", rawJson);
                        HackerNewsItem item = objectMapper.readValue(rawJson, HackerNewsItem.class);
                        
                        // 验证解析后的每个重要字段
                        logger.info("解析后对象: id={}, by={}, title={}, url={}, type={}, time={}",
                            item.getId(), item.getBy(), item.getTitle(), item.getUrl(), 
                            item.getType(), item.getTime());
                        
                        if (item == null) {
                            logger.error("JSON解析为null对象，ID: {}", id);
                        } else if (item.getId() == null) {
                            logger.error("解析的对象没有ID，设置为请求ID: {}", id);
                            item.setId(id);
                        }
                        
                        if (item != null && item.getTitle() != null) {
                            logger.info("成功解析新闻项 ID:{}, 标题: {}", id, item.getTitle());
                        } else if (item != null) {
                            logger.warn("解析新闻项成功 ID:{}, 但标题为null", id);
                        }
                        
                        return Mono.just(item);
                    } catch (Exception e) {
                        logger.error("解析JSON时出错，ID: {}, 错误: {} - {}", id, e.getClass().getName(), e.getMessage());
                        logger.error("原始JSON: {}", rawJson);
                        
                        // 创建空对象作为备用
                        HackerNewsItem emptyItem = new HackerNewsItem();
                        emptyItem.setId(id);
                        return Mono.just(emptyItem);
                    }
                })
                .timeout(Duration.ofSeconds(120))  // 增加超时时间到120秒
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))  // 减少重试次数但增加间隔
                        .maxBackoff(Duration.ofSeconds(20))
                        .doBeforeRetry(retrySignal -> 
                            logger.info("重试获取新闻项 ID:{} (第{}次)", id, retrySignal.totalRetries() + 1)))
                .doOnError(e -> logger.error("获取新闻项 ID:{} 失败: {} - {}", id, e.getClass().getName(), e.getMessage()))
                .onErrorResume(e -> {
                    logger.error("创建空新闻项代替 ID:{}, 错误: {}", id, e.getMessage());
                    HackerNewsItem emptyItem = new HackerNewsItem();
                    emptyItem.setId(id);  // 确保ID被设置
                    return Mono.just(emptyItem);
                });
    }
    
    // 内部类表示HN API返回的数据结构
    // 添加JsonIgnoreProperties注解，允许未知属性
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HackerNewsItem {
        private Long id;
        // 直接使用API返回的字段名
        private String by;        // 作者
        private String title;
        private String url;
        private String text;
        private String type;
        private Long time;
        private List<Long> kids;  // 评论ID列表
        // 添加API中的其他字段
        private Integer score;
        private Integer descendants; // 评论数量
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getBy() {
            return by;
        }
        
        public void setBy(String by) {
            this.by = by;
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
        
        public Integer getScore() {
            return score;
        }
        
        public void setScore(Integer score) {
            this.score = score;
        }
        
        public Integer getDescendants() {
            return descendants;
        }
        
        public void setDescendants(Integer descendants) {
            this.descendants = descendants;
        }
    }
} 