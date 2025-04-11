package com.example.hacker_cnews.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.hacker_cnews.entity.NewsItem;
import com.example.hacker_cnews.repository.NewsItemRepository;
import com.example.hacker_cnews.service.CacheService;
import com.example.hacker_cnews.service.TranslationService;
import com.example.hacker_cnews.service.NewsUpdateService;
import com.example.hacker_cnews.service.HackerNewsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import org.springframework.http.HttpStatus;
import java.time.Duration;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    @Autowired
    private TranslationService translationService;
    
    @Autowired
    private NewsItemRepository repository;
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private NewsUpdateService newsUpdateService;
    
    @Autowired
    private HackerNewsService hackerNewsService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @GetMapping
    public String test() {
        return "API 正常工作!";
    }
    
    /**
     * 数据库连接测试端点
     * 用法示例: /api/test/db-test
     */
    @GetMapping("/db-test")
    public String testDatabase() {
        try {
            long count = repository.count();
            return "数据库连接正常，translated_news 表中有 " + count + " 条记录";
        } catch (Exception e) {
            return "数据库连接错误: " + e.getMessage();
        }
    }
    
    /**
     * Redis缓存测试端点
     * 用法示例: /api/test/cache-test?key=test&value=测试值
     */
    @GetMapping("/cache-test")
    public String testCache(@RequestParam String key, @RequestParam String value) {
        try {
            // 测试写入缓存
            cacheService.cacheTranslation(key, value);
            
            // 测试读取缓存
            String cachedValue = cacheService.getCachedTranslation(key);
            
            if (value.equals(cachedValue)) {
                return "Redis缓存测试成功! 键: " + key + ", 值: " + cachedValue;
            } else {
                return "Redis缓存测试失败! 预期值: " + value + ", 实际值: " + cachedValue;
            }
        } catch (Exception e) {
            return "Redis缓存测试错误: " + e.getMessage();
        }
    }
    
    /**
     * 测试NewsItem对象的缓存功能
     * 用法示例: /api/test/cache-news-test?id=12345
     */
    @GetMapping("/cache-news-test")
    public String testNewsCache(@RequestParam Long id) {
        try {
            // 创建一个测试用的NewsItem
            NewsItem testItem = new NewsItem();
            testItem.setId(id);
            testItem.setTitleEn("Test Title");
            testItem.setTitleZh("测试标题");
            testItem.setCreatedAt(Instant.now());
            testItem.setTime(System.currentTimeMillis() / 1000);
            testItem.setType("story");
            
            // 缓存NewsItem
            cacheService.cacheNewsItem(id, testItem);
            
            // 从缓存获取NewsItem
            NewsItem cachedItem = cacheService.getCachedNewsItem(id);
            
            if (cachedItem != null) {
                return "新闻缓存测试成功! 缓存的标题: " + cachedItem.getTitleZh();
            } else {
                return "新闻缓存测试失败! 无法从缓存中获取新闻项";
            }
        } catch (Exception e) {
            return "新闻缓存测试错误: " + e.getMessage();
        }
    }
    
    /**
     * 简单翻译测试端点
     * 用法示例: /api/test/translate?text=Hello%20World
     */
    @GetMapping("/translate")
    public Mono<String> translate(@RequestParam String text) {
        return translationService.translateEnToZh(text);
    }
    
    /**
     * 带HTML包装的翻译测试端点，便于在浏览器中显示
     * 用法示例: /api/test/translate-html?text=Hello%20World
     */
    @GetMapping(value = "/translate-html", produces = "text/html")
    public Mono<String> translateHtml(@RequestParam String text) {
        return translationService.translateEnToZh(text)
                .map(result -> {
                    StringBuilder html = new StringBuilder();
                    html.append("<!DOCTYPE html><html><head><title>翻译测试</title>");
                    html.append("<meta charset=\"UTF-8\"><style>");
                    html.append("body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}");
                    html.append(".card{border:1px solid #ddd;border-radius:8px;padding:15px;margin-top:20px;}");
                    html.append(".original{color:#333;}.translated{color:#0066cc;font-weight:bold;font-size:1.2em;}");
                    html.append("</style></head><body>");
                    html.append("<h1>百度翻译API测试</h1>");
                    html.append("<div class=\"card\">");
                    html.append("<p>原文: <span class=\"original\">").append(text).append("</span></p>");
                    html.append("<p>译文: <span class=\"translated\">").append(result).append("</span></p>");
                    html.append("</div>");
                    html.append("<p>提示: 修改URL中的text参数可翻译不同文本</p>");
                    html.append("</body></html>");
                    return html.toString();
                });
    }
    
    /**
     * 手动触发新闻更新
     * 用法示例: /api/test/trigger-update
     */
    @GetMapping("/trigger-update")
    public String triggerNewsUpdate() {
        try {
            // 手动触发更新
            newsUpdateService.updateNews();
            return "已手动触发新闻更新，请稍后查看结果";
        } catch (Exception e) {
            return "触发更新失败: " + e.getMessage();
        }
    }
    
    /**
     * 创建测试新闻数据
     * 用法示例: /api/test/create-test-data
     */
    @GetMapping("/create-test-data")
    public String createTestData() {
        try {
            // 创建5条测试新闻
            List<NewsItem> testItems = new ArrayList<>();
            
            for (int i = 1; i <= 5; i++) {
                NewsItem item = new NewsItem();
                item.setId(1000000L + i); // 使用不太可能与实际新闻ID冲突的ID
                item.setTitleEn("Test News Item " + i);
                item.setTitleZh("测试新闻 " + i);
                item.setUrl("https://example.com/test-" + i);
                item.setType("story");
                item.setTime(System.currentTimeMillis() / 1000);
                item.setCreatedAt(Instant.now());
                
                // 添加关键字段，避免首页错误
                item.setRank(i);  // 设置排名
                item.setScore(100 - i * 10);  // 设置一个模拟的分数
                item.setLastUpdated(Instant.now());  // 设置最后更新时间
                
                testItems.add(item);
                
                // 同时添加到缓存
                cacheService.cacheNewsItem(item.getId(), item);
            }
            
            // 保存到数据库
            repository.saveAll(testItems);
            
            return "成功创建5条测试新闻数据";
        } catch (Exception e) {
            return "创建测试数据失败: " + e.getMessage();
        }
    }
    
    /**
     * 检查Hacker News API状态
     * 用法示例: /api/test/check-hn-api
     */
    @GetMapping("/check-hn-api")
    public Mono<String> checkHackerNewsApi() {
        return hackerNewsService.getTopStories(5)
                .map(ids -> {
                    if (ids.isEmpty()) {
                        return "Hacker News API连接失败: 未能获取故事ID列表";
                    }
                    
                    return "Hacker News API连接成功! 获取到 " + ids.size() + " 条新闻ID: " + ids;
                })
                .onErrorResume(e -> Mono.just("Hacker News API连接错误: " + e.getMessage()));
    }
    
    /**
     * 手动获取并保存一个特定的Hacker News项目
     * 用法示例: /api/test/fetch-one?id=37818391
     */
    @GetMapping("/fetch-one")
    public Mono<HackerNewsService.HackerNewsItem> testFetchOne(@RequestParam(defaultValue = "43573156") Long id) {
        logger.info("Test fetch-one for id: {}", id);
        return hackerNewsService.getItemById(id);
    }
    
    /**
     * 手动获取多条Hacker News热门故事
     * 用法示例: /api/test/fetch-top?count=5
     */
    @GetMapping("/fetch-top")
    public Mono<List<Long>> testFetchTop(@RequestParam(defaultValue = "5") int count) {
        logger.info("Test fetch-top with count: {}", count);
        return hackerNewsService.getTopStories(count);
    }
    
    /**
     * 直接测试Hacker News API连接和解析
     * 用法示例: /api/test/direct-api-test?id=43573156
     */
    @GetMapping("/direct-api-test")
    public Mono<Map<String, Object>> testDirectApiAccess() {
        logger.info("执行直接API访问测试");
        WebClient testClient = WebClient.builder()
                .baseUrl("https://hacker-news.firebaseio.com/v0")
                .build();
        
        return testClient.get()
                .uri("/topstories.json")
                .retrieve()
                .bodyToMono(List.class)
                .flatMap(topStories -> {
                    if (topStories == null || topStories.isEmpty()) {
                        Map<String, Object> errorMap = new HashMap<>();
                        errorMap.put("error", "No top stories found");
                        return Mono.just(errorMap);
                    }
                    
                    logger.info("获取到 {} 条热门故事ID", topStories.size());
                    Object firstId = topStories.get(0);
                    
                    return testClient.get()
                            .uri("/item/{id}.json", firstId)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .map(item -> {
                                Map<String, Object> resultMap = new HashMap<>();
                                resultMap.put("topStories", topStories.subList(0, Math.min(5, topStories.size())));
                                resultMap.put("firstItem", item);
                                return resultMap;
                            });
                });
    }
    
    /**
     * 测试修改后的HackerNewsService
     * 用法示例: /api/test/improved-fetch-one?id=43573156
     */
    @GetMapping("/improved-fetch-one")
    public Mono<Map<String, Object>> improvedFetchOne(@RequestParam(defaultValue = "43573156") Long id) {
        logger.info("改进的测试端点 - 获取并解析单个项目，ID: {}", id);
        
        return hackerNewsService.getItemById(id)
                .map(item -> {
                    logger.info("获取到项目: ID={}, 标题={}", item.getId(), item.getTitle());
                    
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("id", item.getId() != null ? item.getId() : "null");
                    resultMap.put("title", item.getTitle() != null ? item.getTitle() : "null");
                    resultMap.put("by", item.getBy() != null ? item.getBy() : "null");
                    resultMap.put("url", item.getUrl() != null ? item.getUrl() : "null");
                    resultMap.put("type", item.getType() != null ? item.getType() : "null");
                    resultMap.put("time", item.getTime() != null ? item.getTime() : "null");
                    resultMap.put("score", item.getScore() != null ? item.getScore() : "null");
                    resultMap.put("descendants", item.getDescendants() != null ? item.getDescendants() : "null");
                    resultMap.put("kids_count", item.getKids() != null ? item.getKids().size() : 0);
                    return resultMap;
                })
                .onErrorResume(e -> {
                    logger.error("获取项目时出错: {}", e.getMessage());
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", e.getMessage());
                    return Mono.just(errorMap);
                });
    }
    
    @GetMapping("/check-fields-mapping")
    public Mono<ResponseEntity<String>> checkFieldsMapping(@RequestParam(defaultValue = "43573156") Long id) {
        logger.info("检查字段映射测试 - 获取项目 ID: {}", id);
        
        return WebClient.builder()
                .baseUrl("https://hacker-news.firebaseio.com/v0")
                .build()
                .get()
                .uri("/item/{id}.json", id)
                .retrieve()
                .bodyToMono(String.class)
                .map(rawJson -> {
                    try {
                        logger.info("原始JSON: {}", rawJson);
                        HackerNewsService.HackerNewsItem item = objectMapper.readValue(rawJson, HackerNewsService.HackerNewsItem.class);
                        
                        StringBuilder result = new StringBuilder();
                        result.append("<!DOCTYPE html>\n<html>\n<head>\n");
                        result.append("<meta charset=\"UTF-8\">\n");
                        result.append("<title>字段映射测试</title>\n</head>\n<body>\n");
                        result.append("<h1>字段映射测试</h1>");
                        result.append("<h2>原始JSON</h2>");
                        result.append("<pre>").append(rawJson).append("</pre>");
                        
                        result.append("<h2>解析后的对象</h2>");
                        result.append("<ul>");
                        result.append("<li>ID: ").append(item.getId()).append("</li>");
                        result.append("<li>By: ").append(item.getBy()).append("</li>");
                        result.append("<li>Title: ").append(item.getTitle()).append("</li>");
                        result.append("<li>URL: ").append(item.getUrl()).append("</li>");
                        result.append("<li>Text: ").append(item.getText()).append("</li>");
                        result.append("<li>Type: ").append(item.getType()).append("</li>");
                        result.append("<li>Time: ").append(item.getTime()).append("</li>");
                        result.append("<li>Score: ").append(item.getScore()).append("</li>");
                        result.append("<li>Descendants: ").append(item.getDescendants()).append("</li>");
                        result.append("<li>Kids: ").append(item.getKids() != null ? item.getKids().size() : 0).append(" 条评论</li>");
                        result.append("</ul>");
                        
                        result.append("<p><a href='/api/test/process-item?id=").append(id).append("'>处理这个项目</a></p>");
                        result.append("</body>\n</html>");
                        
                        return ResponseEntity.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .header("Content-Type", "text/html; charset=UTF-8")
                                .body(result.toString());
                    } catch (Exception e) {
                        logger.error("处理响应时出错: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.TEXT_PLAIN)
                                .body("处理响应时出错: " + e.getMessage());
                    }
                })
                .onErrorResume(e -> {
                    logger.error("获取项目时出错: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body("获取项目时出错: " + e.getMessage()));
                });
    }
    
    @GetMapping("/process-item")
    public Mono<ResponseEntity<String>> processItem(@RequestParam(defaultValue = "43573156") Long id) {
        logger.info("手动处理新闻项目 ID: {}", id);
        
        return hackerNewsService.getItemById(id)
                .flatMap(hnItem -> {
                    StringBuilder result = new StringBuilder();
                    result.append("<!DOCTYPE html>\n<html>\n<head>\n");
                    result.append("<meta charset=\"UTF-8\">\n");
                    result.append("<title>处理新闻项目</title>\n");
                    result.append("<style>body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}</style>\n");
                    result.append("</head>\n<body>\n");
                    result.append("<h1>处理结果</h1>");
                    
                    // 从服务获取的项目
                    result.append("<h2>从HackerNewsService获取的项目</h2>");
                    result.append("<ul>");
                    result.append("<li>ID: ").append(hnItem.getId()).append("</li>");
                    result.append("<li>By: ").append(hnItem.getBy()).append("</li>");
                    result.append("<li>Title: ").append(hnItem.getTitle()).append("</li>");
                    result.append("<li>URL: ").append(hnItem.getUrl()).append("</li>");
                    result.append("<li>Text: ").append(hnItem.getText()).append("</li>");
                    result.append("<li>Type: ").append(hnItem.getType()).append("</li>");
                    result.append("<li>Time: ").append(hnItem.getTime()).append("</li>");
                    result.append("<li>Score: ").append(hnItem.getScore()).append("</li>");
                    result.append("<li>Descendants: ").append(hnItem.getDescendants()).append("</li>");
                    result.append("</ul>");
                    
                    // 如果标题不存在，直接返回错误
                    if (hnItem.getTitle() == null) {
                        result.append("<h2>错误：标题为空，无法保存！</h2>");
                        result.append("</body>\n</html>");
                        return Mono.just(ResponseEntity.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .header("Content-Type", "text/html; charset=UTF-8")
                                .body(result.toString()));
                    }
                    
                    // 开始翻译标题
                    result.append("<h2>翻译处理</h2>");
                    return translationService.translateEnToZh(hnItem.getTitle())
                            .flatMap(translatedTitle -> {
                                result.append("<p>标题翻译: ").append(translatedTitle).append("</p>");
                                
                                Mono<String> textTranslation = Mono.just("");
                                if (hnItem.getText() != null && !hnItem.getText().isEmpty()) {
                                    return translationService.translateEnToZh(hnItem.getText())
                                            .map(translatedText -> {
                                                result.append("<p>文本翻译: ").append(translatedText).append("</p>");
                                                
                                                // 检查数据库中是否已存在该新闻
                                                NewsItem newsItem;
                                                Integer rank = 1; // 默认排名
                                                
                                                if (repository.existsById(hnItem.getId())) {
                                                    // 如果存在，获取现有记录并更新
                                                    newsItem = repository.findById(hnItem.getId()).orElse(new NewsItem());
                                                    // 保留原有排名
                                                    rank = newsItem.getRank() != null ? newsItem.getRank() : 1;
                                                    logger.info("更新已存在的新闻项: ID={}, 当前排名={}", hnItem.getId(), rank);
                                                } else {
                                                    // 如果不存在，创建新记录
                                                    newsItem = new NewsItem();
                                                    newsItem.setCreatedAt(Instant.now());
                                                    logger.info("创建新的新闻项: ID={}", hnItem.getId());
                                                }
                                                
                                                // 设置或更新字段
                                                newsItem.setId(hnItem.getId());
                                                newsItem.setTitleEn(hnItem.getTitle());
                                                newsItem.setTitleZh(translatedTitle);
                                                newsItem.setUrl(hnItem.getUrl());
                                                newsItem.setTextEn(hnItem.getText());
                                                newsItem.setTextZh(translatedText);
                                                newsItem.setType(hnItem.getType());
                                                newsItem.setTime(hnItem.getTime());
                                                // 设置/更新关键字段
                                                newsItem.setScore(hnItem.getScore());
                                                newsItem.setRank(rank);
                                                newsItem.setLastUpdated(Instant.now());
                                                
                                                // 保存到数据库
                                                repository.save(newsItem);
                                                
                                                // 添加结果标题和描述
                                                result.append("<h2>保存结果</h2>");
                                                result.append("<p>新闻项已成功保存到数据库！</p>");
                                                
                                                // 添加详细保存信息表格
                                                result.append("<table border='1' style='border-collapse: collapse; width: 100%; margin-top: 15px;'>");
                                                result.append("<tr><th>字段</th><th>值</th></tr>");
                                                result.append("<tr><td>ID</td><td>").append(newsItem.getId()).append("</td></tr>");
                                                result.append("<tr><td>标题(英文)</td><td>").append(newsItem.getTitleEn()).append("</td></tr>");
                                                result.append("<tr><td>标题(中文)</td><td>").append(newsItem.getTitleZh()).append("</td></tr>");
                                                result.append("<tr><td>URL</td><td>").append(newsItem.getUrl()).append("</td></tr>");
                                                result.append("<tr><td>类型</td><td>").append(newsItem.getType()).append("</td></tr>");
                                                result.append("<tr><td>时间</td><td>").append(newsItem.getTime()).append("</td></tr>");
                                                result.append("<tr><td>分数</td><td>").append(newsItem.getScore()).append("</td></tr>");
                                                result.append("<tr><td>排名</td><td>").append(newsItem.getRank()).append("</td></tr>");
                                                result.append("<tr><td>创建时间</td><td>").append(newsItem.getCreatedAt()).append("</td></tr>");
                                                result.append("<tr><td>最后更新</td><td>").append(newsItem.getLastUpdated()).append("</td></tr>");
                                                result.append("</table>");
                                                
                                                result.append("<p><a href='/'>返回首页查看</a></p>");
                                                result.append("</body>\n</html>");
                                                
                                                return ResponseEntity.ok()
                                                        .contentType(MediaType.TEXT_HTML)
                                                        .header("Content-Type", "text/html; charset=UTF-8")
                                                        .body(result.toString());
                                            });
                                } else {
                                    // 没有文本内容，直接创建新闻项
                                    // 检查数据库中是否已存在该新闻
                                    NewsItem newsItem;
                                    Integer rank = 1; // 默认排名
                                    
                                    if (repository.existsById(hnItem.getId())) {
                                        // 如果存在，获取现有记录并更新
                                        newsItem = repository.findById(hnItem.getId()).orElse(new NewsItem());
                                        // 保留原有排名
                                        rank = newsItem.getRank() != null ? newsItem.getRank() : 1;
                                        logger.info("更新已存在的新闻项: ID={}, 当前排名={}", hnItem.getId(), rank);
                                    } else {
                                        // 如果不存在，创建新记录
                                        newsItem = new NewsItem();
                                        newsItem.setCreatedAt(Instant.now());
                                        logger.info("创建新的新闻项: ID={}", hnItem.getId());
                                    }
                                    
                                    // 设置或更新字段
                                    newsItem.setId(hnItem.getId());
                                    newsItem.setTitleEn(hnItem.getTitle());
                                    newsItem.setTitleZh(translatedTitle);
                                    newsItem.setUrl(hnItem.getUrl());
                                    newsItem.setType(hnItem.getType());
                                    newsItem.setTime(hnItem.getTime());
                                    // 设置/更新关键字段
                                    newsItem.setScore(hnItem.getScore());
                                    newsItem.setRank(rank);
                                    newsItem.setLastUpdated(Instant.now());
                                    
                                    // 保存到数据库
                                    repository.save(newsItem);
                                    result.append("<h2>保存结果</h2>");
                                    result.append("<p>新闻项已成功保存到数据库！</p>");
                                    
                                    // 添加详细保存信息表格
                                    result.append("<table border='1' style='border-collapse: collapse; width: 100%; margin-top: 15px;'>");
                                    result.append("<tr><th>字段</th><th>值</th></tr>");
                                    result.append("<tr><td>ID</td><td>").append(newsItem.getId()).append("</td></tr>");
                                    result.append("<tr><td>标题(英文)</td><td>").append(newsItem.getTitleEn()).append("</td></tr>");
                                    result.append("<tr><td>标题(中文)</td><td>").append(newsItem.getTitleZh()).append("</td></tr>");
                                    result.append("<tr><td>URL</td><td>").append(newsItem.getUrl()).append("</td></tr>");
                                    result.append("<tr><td>类型</td><td>").append(newsItem.getType()).append("</td></tr>");
                                    result.append("<tr><td>时间</td><td>").append(newsItem.getTime()).append("</td></tr>");
                                    result.append("<tr><td>分数</td><td>").append(newsItem.getScore()).append("</td></tr>");
                                    result.append("<tr><td>排名</td><td>").append(newsItem.getRank()).append("</td></tr>");
                                    result.append("<tr><td>创建时间</td><td>").append(newsItem.getCreatedAt()).append("</td></tr>");
                                    result.append("<tr><td>最后更新</td><td>").append(newsItem.getLastUpdated()).append("</td></tr>");
                                    result.append("</table>");
                                    
                                    result.append("<p><a href='/'>返回首页查看</a></p>");
                                    result.append("</body>\n</html>");
                                    
                                    return Mono.just(ResponseEntity.ok()
                                            .contentType(MediaType.TEXT_HTML)
                                            .header("Content-Type", "text/html; charset=UTF-8")
                                            .body(result.toString()));
                                }
                            })
                            .onErrorResume(e -> {
                                logger.error("翻译时出错: {}", e.getMessage());
                                result.append("<h2>翻译错误</h2>");
                                result.append("<p>").append(e.getMessage()).append("</p>");
                                result.append("</body>\n</html>");
                                return Mono.just(ResponseEntity.ok()
                                        .contentType(MediaType.TEXT_HTML)
                                        .header("Content-Type", "text/html; charset=UTF-8")
                                        .body(result.toString()));
                            });
                })
                .onErrorResume(e -> {
                    logger.error("处理项目时出错: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.TEXT_PLAIN)
                            .header("Content-Type", "text/plain; charset=UTF-8")
                            .body("处理项目时出错: " + e.getMessage()));
                });
    }
    
    @GetMapping("/manual-update")
    public Mono<ResponseEntity<String>> manualUpdate() {
        logger.info("开始手动更新新闻数据");
        newsUpdateService.updateNews();
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header("Content-Type", "text/html; charset=UTF-8")
                .body("<!DOCTYPE html>\n<html>\n<head>\n" +
                      "<meta charset=\"UTF-8\">\n" +
                      "<title>手动更新</title>\n" +
                      "<style>body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}</style>\n" +
                      "</head>\n<body>\n" +
                      "<h1>手动更新已触发</h1>" +
                      "<p>请查看控制台日志以获取详细信息</p>" +
                      "<p><a href='/'>返回首页</a></p>" +
                      "</body>\n</html>"));
    }
    
    /**
     * 测试修复后的百度翻译API
     * 用法示例: /api/test/test-translation?text=Hello%20World
     */
    @GetMapping("/test-translation")
    public Mono<ResponseEntity<String>> testTranslation(@RequestParam(defaultValue = "Hello, this is a test!") String text) {
        logger.info("测试修复后的百度翻译API，文本: {}", text);
        
        return translationService.translateEnToZh(text)
                .map(result -> {
                    StringBuilder html = new StringBuilder();
                    html.append("<!DOCTYPE html><html><head><title>翻译API测试</title>");
                    html.append("<meta charset=\"UTF-8\"><style>");
                    html.append("body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}");
                    html.append(".card{border:1px solid #ddd;border-radius:8px;padding:15px;margin-top:20px;}");
                    html.append(".original{color:#333;}.translated{color:#0066cc;font-weight:bold;font-size:1.2em;}");
                    html.append(".success{color:green;}.error{color:red;}");
                    html.append("</style></head><body>");
                    html.append("<h1>百度翻译API测试</h1>");
                    html.append("<div class=\"card\">");
                    html.append("<p>原文: <span class=\"original\">").append(text).append("</span></p>");
                    
                    if (result.startsWith("翻译出错")) {
                        html.append("<p class=\"error\">").append(result).append("</p>");
                        html.append("<h2>可能的解决方案</h2>");
                        html.append("<ol>");
                        html.append("<li>检查百度翻译API的appid和key是否正确</li>");
                        html.append("<li>确认百度翻译API账户是否有足够的配额</li>");
                        html.append("<li>检查服务器时间是否准确同步</li>");
                        html.append("<li>查看服务器日志以获取更多信息</li>");
                        html.append("</ol>");
                    } else {
                        html.append("<p>译文: <span class=\"translated\">").append(result).append("</span></p>");
                        html.append("<p class=\"success\">翻译成功!</p>");
                    }
                    
                    html.append("</div>");
                    html.append("<p>配置信息:</p>");
                    html.append("<ul>");
                    html.append("<li>API URL: https://fanyi-api.baidu.com/api/trans/vip/translate</li>");
                    html.append("<li>请在application.properties中配置百度翻译API的appid和key</li>");
                    html.append("</ul>");
                    
                    html.append("<h2>测试其他文本</h2>");
                    html.append("<form action=\"/api/test/test-translation\" method=\"get\">");
                    html.append("<textarea name=\"text\" rows=\"4\" cols=\"50\">").append(text).append("</textarea><br>");
                    html.append("<input type=\"submit\" value=\"翻译\">");
                    html.append("</form>");
                    
                    html.append("<p><a href='/'>返回首页</a></p>");
                    html.append("</body></html>");
                    
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(html.toString());
                });
    }
    
    /**
     * 清空数据库并重新获取新闻
     * 用法示例: /api/test/reset-and-update
     */
    @GetMapping("/reset-and-update")
    public Mono<ResponseEntity<String>> resetAndUpdate() {
        logger.info("清空数据库并重新获取新闻");
        
        try {
            // 清空数据库
            repository.deleteAll();
            logger.info("数据库已清空");
            
            // 触发新闻更新
            newsUpdateService.updateNews();
            logger.info("已触发新闻更新");
            
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<!DOCTYPE html>\n<html>\n<head>\n" +
                          "<meta charset=\"UTF-8\">\n" +
                          "<title>重置数据库</title>\n" +
                          "<style>body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}</style>\n" +
                          "</head>\n<body>\n" +
                          "<h1>数据库已清空并触发更新</h1>" +
                          "<p>请等待几分钟，然后<a href='/'>刷新首页</a>查看结果</p>" +
                          "<p>或者查看<a href='/api/test/manual-update'>手动更新状态</a></p>" +
                          "</body>\n</html>"));
        } catch (Exception e) {
            logger.error("重置和更新过程中出错: {}", e.getMessage(), e);
            
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<!DOCTYPE html>\n<html>\n<head>\n" +
                          "<meta charset=\"UTF-8\">\n" +
                          "<title>操作失败</title>\n" +
                          "<style>body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}</style>\n" +
                          "</head>\n<body>\n" +
                          "<h1>操作失败</h1>" +
                          "<p>清空数据库或触发更新时出错: " + e.getMessage() + "</p>" +
                          "<p><a href='/api/test/dashboard'>返回测试面板</a></p>" +
                          "</body>\n</html>"));
        }
    }
    
    /**
     * 测试工具总览页面，展示所有可用的测试端点
     * 用法示例: /api/test/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<String> testDashboard() {
        logger.info("访问测试控制面板");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Hacker C News - 测试面板</title>\n");
        html.append("    <style>\n");
        html.append("        body {\n");
        html.append("            font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;\n");
        html.append("            line-height: 1.6;\n");
        html.append("            color: #333;\n");
        html.append("            max-width: 900px;\n");
        html.append("            margin: 0 auto;\n");
        html.append("            padding: 20px;\n");
        html.append("        }\n");
        html.append("        h1 {\n");
        html.append("            color: #ff6600;\n");
        html.append("        }\n");
        html.append("        table {\n");
        html.append("            width: 100%;\n");
        html.append("            border-collapse: collapse;\n");
        html.append("            margin: 20px 0;\n");
        html.append("        }\n");
        html.append("        th, td {\n");
        html.append("            padding: 10px;\n");
        html.append("            border: 1px solid #ddd;\n");
        html.append("            text-align: left;\n");
        html.append("        }\n");
        html.append("        th {\n");
        html.append("            background-color: #f5f5f5;\n");
        html.append("        }\n");
        html.append("        tr:nth-child(even) {\n");
        html.append("            background-color: #f9f9f9;\n");
        html.append("        }\n");
        html.append("        .highlight {\n");
        html.append("            background-color: #fffde7;\n");
        html.append("            font-weight: bold;\n");
        html.append("        }\n");
        html.append("        .highlight td {\n");
        html.append("            border-color: #ffecb3;\n");
        html.append("        }\n");
        html.append("        .danger {\n");
        html.append("            background-color: #ffebee;\n");
        html.append("            font-weight: bold;\n");
        html.append("        }\n");
        html.append("        .danger td {\n");
        html.append("            border-color: #ffcdd2;\n");
        html.append("        }\n");
        html.append("        .btn-danger {\n");
        html.append("            background-color: #f44336;\n");
        html.append("            color: white;\n");
        html.append("            padding: 8px 12px;\n");
        html.append("            text-decoration: none;\n");
        html.append("            border-radius: 4px;\n");
        html.append("            display: inline-block;\n");
        html.append("        }\n");
        html.append("        .btn-warning {\n");
        html.append("            background-color: #ff9800;\n");
        html.append("            color: white;\n");
        html.append("            padding: 8px 12px;\n");
        html.append("            text-decoration: none;\n");
        html.append("            border-radius: 4px;\n");
        html.append("            display: inline-block;\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <h1>Hacker C News 测试控制面板</h1>\n");
        html.append("    <p>此面板提供了各种测试功能，帮助诊断和调试系统</p>\n");
        html.append("    \n");
        html.append("    <h2>基本功能测试</h2>\n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th width=\"20%\">功能</th>\n");
        html.append("            <th width=\"50%\">描述</th>\n");
        html.append("            <th width=\"30%\">操作</th>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>基本测试</td>\n");
        html.append("            <td>测试API端点是否正常工作</td>\n");
        html.append("            <td><a href=\"/api/test\" target=\"_blank\">测试</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>数据库测试</td>\n");
        html.append("            <td>测试数据库连接是否正常工作</td>\n");
        html.append("            <td><a href=\"/api/test/db-test\" target=\"_blank\">测试</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>缓存测试</td>\n");
        html.append("            <td>测试Redis缓存是否正常工作</td>\n");
        html.append("            <td>\n");
        html.append("                <form action=\"/api/test/cache-test\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"text\" name=\"key\" placeholder=\"键\" required>\n");
        html.append("                    <input type=\"text\" name=\"value\" placeholder=\"值\" required>\n");
        html.append("                    <button type=\"submit\">测试</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>新闻缓存测试</td>\n");
        html.append("            <td>测试新闻对象的缓存功能</td>\n");
        html.append("            <td>\n");
        html.append("                <form action=\"/api/test/cache-news-test\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"number\" name=\"id\" placeholder=\"新闻ID\" required>\n");
        html.append("                    <button type=\"submit\">测试</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("    </table>\n");
        html.append("    \n");
        html.append("    <h2>翻译功能测试</h2>\n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th width=\"20%\">功能</th>\n");
        html.append("            <th width=\"50%\">描述</th>\n");
        html.append("            <th width=\"30%\">操作</th>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>翻译测试</td>\n");
        html.append("            <td>测试百度翻译API</td>\n");
        html.append("            <td>\n");
        html.append("                <form action=\"/api/test/translate\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"text\" name=\"text\" placeholder=\"输入英文\" required>\n");
        html.append("                    <button type=\"submit\">翻译</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>HTML格式翻译</td>\n");
        html.append("            <td>以HTML格式显示翻译结果</td>\n");
        html.append("            <td>\n");
        html.append("                <form action=\"/api/test/translate-html\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"text\" name=\"text\" placeholder=\"输入英文\" required>\n");
        html.append("                    <button type=\"submit\">翻译</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>完整翻译测试</td>\n");
        html.append("            <td>测试翻译服务的完整功能</td>\n");
        html.append("            <td><a href=\"/api/test/test-translation\" target=\"_blank\">默认测试</a> | \n");
        html.append("                <form style=\"display:inline;\" action=\"/api/test/test-translation\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"text\" name=\"text\" placeholder=\"输入英文\" style=\"width:120px;\">\n");
        html.append("                    <button type=\"submit\">测试</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("        <tr class=\"highlight\">\n");
        html.append("            <td>百度vs DeepL对比</td>\n");
        html.append("            <td>比较百度翻译和DeepL翻译的结果质量</td>\n");
        html.append("            <td><a href=\"/api/test/compare-translation\" target=\"_blank\">默认测试</a> | \n");
        html.append("                <form style=\"display:inline;\" action=\"/api/test/compare-translation\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"text\" name=\"text\" placeholder=\"输入英文\" style=\"width:120px;\">\n");
        html.append("                    <button type=\"submit\">对比翻译</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("    </table>\n");
        html.append("    \n");
        html.append("    <h2>Hacker News API 测试</h2>\n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th width=\"20%\">功能</th>\n");
        html.append("            <th width=\"50%\">描述</th>\n");
        html.append("            <th width=\"30%\">操作</th>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>检查API连接</td>\n");
        html.append("            <td>检查与Hacker News API的连接是否正常</td>\n");
        html.append("            <td><a href=\"/api/test/check-hn-api\" target=\"_blank\">检查</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>获取单个项目</td>\n");
        html.append("            <td>从Hacker News API获取单个新闻项</td>\n");
        html.append("            <td>\n");
        html.append("                <a href=\"/api/test/fetch-one?id=43573156\" target=\"_blank\">获取默认ID</a> | \n");
        html.append("                <form style=\"display:inline;\" action=\"/api/test/fetch-one\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"number\" name=\"id\" placeholder=\"输入ID\" style=\"width:80px;\">\n");
        html.append("                    <button type=\"submit\">获取</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>获取热门故事</td>\n");
        html.append("            <td>获取Hacker News热门故事ID列表</td>\n");
        html.append("            <td>\n");
        html.append("                <form action=\"/api/test/fetch-top\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"number\" name=\"count\" placeholder=\"数量\" value=\"5\" style=\"width:60px;\">\n");
        html.append("                    <button type=\"submit\">获取</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>直接API访问</td>\n");
        html.append("            <td>测试更复杂的API访问场景</td>\n");
        html.append("            <td><a href=\"/api/test/direct-api-test\" target=\"_blank\">测试</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>改进的单一项目获取</td>\n");
        html.append("            <td>使用改进的方法获取单个新闻项，包含更多详细信息</td>\n");
        html.append("            <td>\n");
        html.append("                <a href=\"/api/test/improved-fetch-one?id=43573156\" target=\"_blank\">获取默认ID</a> | \n");
        html.append("                <form style=\"display:inline;\" action=\"/api/test/improved-fetch-one\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"number\" name=\"id\" placeholder=\"输入ID\" style=\"width:80px;\">\n");
        html.append("                    <button type=\"submit\">获取</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>检查字段映射</td>\n");
        html.append("            <td>检查API返回的字段是否正确映射到实体</td>\n");
        html.append("            <td>\n");
        html.append("                <a href=\"/api/test/check-fields-mapping?id=43573156\" target=\"_blank\">检查默认ID</a> | \n");
        html.append("                <form style=\"display:inline;\" action=\"/api/test/check-fields-mapping\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"number\" name=\"id\" placeholder=\"输入ID\" style=\"width:80px;\">\n");
        html.append("                    <button type=\"submit\">检查</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("    </table>\n");
        html.append("    \n");
        html.append("    <h2>数据处理测试</h2>\n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th width=\"20%\">功能</th>\n");
        html.append("            <th width=\"50%\">描述</th>\n");
        html.append("            <th width=\"30%\">操作</th>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>处理单个新闻项</td>\n");
        html.append("            <td>从API获取新闻项，翻译并保存到数据库</td>\n");
        html.append("            <td>\n");
        html.append("                <a href=\"/api/test/process-item?id=43573156\" target=\"_blank\">处理默认ID</a> | \n");
        html.append("                <form style=\"display:inline;\" action=\"/api/test/process-item\" method=\"get\" target=\"_blank\">\n");
        html.append("                    <input type=\"number\" name=\"id\" placeholder=\"输入ID\" style=\"width:80px;\">\n");
        html.append("                    <button type=\"submit\">处理</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>手动触发更新</td>\n");
        html.append("            <td>手动触发新闻数据更新流程</td>\n");
        html.append("            <td><a href=\"/api/test/manual-update\" target=\"_blank\">触发更新</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>创建测试数据</td>\n");
        html.append("            <td>创建一些测试数据用于展示</td>\n");
        html.append("            <td><a href=\"/api/test/create-test-data\" target=\"_blank\">创建数据</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>重置并更新</td>\n");
        html.append("            <td>清空数据库，并从API获取新的数据</td>\n");
        html.append("            <td><a href=\"/api/test/reset-and-update\" target=\"_blank\" class=\"btn-warning\" onclick=\"return confirm('确定要清空数据库并重新获取数据吗？')\">重置并更新</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr class=\"danger\">\n");
        html.append("            <td>彻底重置并更新</td>\n");
        html.append("            <td>清空数据库和Redis缓存，并从API获取新的数据</td>\n");
        html.append("            <td><a href=\"/api/test/complete-reset\" target=\"_blank\" class=\"btn-danger\" onclick=\"return confirm('警告：此操作将清空数据库和所有缓存！确定要继续吗？')\">彻底重置并更新</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr class=\"highlight\">\n");
        html.append("            <td>查看所有新闻</td>\n");
        html.append("            <td>显示数据库中所有新闻记录，包括不完整记录</td>\n");
        html.append("            <td><a href=\"/api/test/view-all-news\" target=\"_blank\">查看</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr class=\"highlight\">\n");
        html.append("            <td>修复不完整数据</td>\n");
        html.append("            <td>扫描并修复数据库中的不完整记录（缺失标题、排名或分数）</td>\n");
        html.append("            <td><a href=\"/api/test/fix-incomplete-data\" target=\"_blank\">修复</a></td>\n");
        html.append("        </tr>\n");
        html.append("        <tr class=\"highlight\">\n");
        html.append("            <td>批量获取更新</td>\n");
        html.append("            <td>使用改进的批处理策略获取和更新新闻数据</td>\n");
        html.append("            <td>\n");
        html.append("                <form action=\"/api/test/batch-fetch\" method=\"get\" target=\"_blank\" style=\"display:inline;\">\n");
        html.append("                    <button type=\"submit\">默认设置</button>\n");
        html.append("                </form> | \n");
        html.append("                <form action=\"/api/test/batch-fetch\" method=\"get\" target=\"_blank\" style=\"display:inline;\">\n");
        html.append("                    数量：<input type=\"number\" name=\"limit\" value=\"50\" style=\"width:40px;\">\n");
        html.append("                    批次：<input type=\"number\" name=\"batchSize\" value=\"10\" style=\"width:40px;\">\n");
        html.append("                    并发：<input type=\"number\" name=\"concurrency\" value=\"3\" style=\"width:40px;\">\n");
        html.append("                    <button type=\"submit\">批量获取</button>\n");
        html.append("                </form>\n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("    </table>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html.toString());
    }
    
    /**
     * 修复不完整的新闻记录
     * 扫描所有缺少标题、排名或分数的记录，从API获取完整数据
     */
    @GetMapping("/fix-incomplete-data")
    public Mono<ResponseEntity<String>> fixIncompleteData() {
        logger.info("开始修复不完整数据");
        
        // 获取所有新闻项
        List<NewsItem> allItems = repository.findAll();
        
        // 过滤出不完整的记录
        List<NewsItem> incompleteItems = allItems.stream()
            .filter(item -> item.getTitleEn() == null || 
                           item.getRank() == null || 
                           item.getScore() == null)
            .collect(Collectors.toList());
        
        // 如果没有不完整的记录，直接返回
        if (incompleteItems.isEmpty()) {
            logger.info("没有找到不完整的记录");
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"zh-CN\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>修复不完整数据 - 结果</title>\n");
            html.append("    <style>\n");
            html.append("        body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; line-height: 1.6; max-width: 900px; margin: 0 auto; padding: 20px; }\n");
            html.append("        h1 { color: #ff6600; }\n");
            html.append("        .success { color: #28a745; }\n");
            html.append("        .back-link { margin-top: 20px; }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <h1>修复不完整数据 - 结果</h1>\n");
            html.append("    <div class=\"success\">\n");
            html.append("        <p>✓ 没有找到不完整的记录。数据库中的记录都是完整的。</p>\n");
            html.append("    </div>\n");
            html.append("    <div class=\"back-link\">\n");
            html.append("        <a href=\"/api/test/dashboard\">返回控制面板</a>\n");
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>\n");
            
            return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html.toString()));
        }
        
        logger.info("找到 {} 条不完整记录，开始修复", incompleteItems.size());
        
        // 准备HTML结果页
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>修复不完整数据 - 结果</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; line-height: 1.6; max-width: 900px; margin: 0 auto; padding: 20px; }\n");
        html.append("        h1 { color: #ff6600; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append("        th, td { padding: 10px; border: 1px solid #ddd; text-align: left; }\n");
        html.append("        th { background-color: #f5f5f5; }\n");
        html.append("        tr:nth-child(even) { background-color: #f9f9f9; }\n");
        html.append("        .success { color: #28a745; }\n");
        html.append("        .error { color: #dc3545; }\n");
        html.append("        .summary { margin: 20px 0; padding: 15px; background-color: #f8f9fa; border-radius: 5px; }\n");
        html.append("        .back-link { margin-top: 20px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <h1>修复不完整数据 - 结果</h1>\n");
        html.append("    <div class=\"summary\">\n");
        html.append("        <p>找到 ").append(incompleteItems.size()).append(" 条不完整记录，正在尝试修复...</p>\n");
        html.append("    </div>\n");
        
        // 创建结果表格
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th>ID</th>\n");
        html.append("            <th>原状态</th>\n");
        html.append("            <th>修复结果</th>\n");
        html.append("        </tr>\n");
        
        // 将不完整记录转换为响应式流
        return Flux.fromIterable(incompleteItems)
            .flatMap(item -> {
                // 为每个记录创建一个包含状态信息的不可变对象
                final long itemId = item.getId();
                final StringBuilder missingFields = new StringBuilder();
                if (item.getTitleEn() == null) missingFields.append("标题 ");
                if (item.getRank() == null) missingFields.append("排名 ");
                if (item.getScore() == null) missingFields.append("分数 ");
                final String missingFieldsStr = missingFields.toString();
                
                // 使用API获取完整数据
                return hackerNewsService.getItemById(itemId)
                    .flatMap(newsDetails -> {
                        // 更新缺失的字段
                        boolean updated = false;
                        
                        if (item.getTitleEn() == null && newsDetails.getTitle() != null) {
                            item.setTitleEn(newsDetails.getTitle());
                            updated = true;
                        }
                        
                        if (item.getScore() == null && newsDetails.getScore() != null) {
                            item.setScore(newsDetails.getScore());
                            updated = true;
                        }
                        
                        // 排名可能需要计算，暂时使用分数作为排名
                        if (item.getRank() == null && newsDetails.getScore() != null) {
                            item.setRank(newsDetails.getScore());
                            updated = true;
                        }
                        
                        // 更新最后更新时间
                        item.setLastUpdated(Instant.now());
                        
                        // 同步方法保存更新后的记录，然后将结果转为Mono
                        NewsItem savedItem = repository.save(item);
                        
                        return Mono.just(generateResultRow(itemId, missingFieldsStr, updated, null));
                    })
                    .onErrorResume(e -> {
                        logger.error("修复ID为{}的记录时出错: {}", itemId, e.getMessage());
                        return Mono.just(generateResultRow(itemId, missingFieldsStr, false, e.getMessage()));
                    });
            })
            .collectList()
            .map(resultRows -> {
                // 将所有结果行添加到HTML表格
                for (String row : resultRows) {
                    html.append(row);
                }
                
                html.append("    </table>\n");
                
                // 添加操作总结
                html.append("    <div class=\"summary\">\n");
                html.append("        <p>修复操作完成。共处理 ").append(incompleteItems.size()).append(" 条记录。</p>\n");
                html.append("    </div>\n");
                
                html.append("    <div class=\"back-link\">\n");
                html.append("        <a href=\"/api/test/dashboard\">返回控制面板</a> | \n");
                html.append("        <a href=\"/api/test/view-all-news\">查看所有新闻</a>\n");
                html.append("    </div>\n");
                
                html.append("</body>\n");
                html.append("</html>\n");
                
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html.toString());
            });
    }
    
    // 辅助方法，生成单行修复结果的HTML
    private String generateResultRow(long id, String missingFields, boolean updated, String errorMessage) {
        StringBuilder result = new StringBuilder();
        result.append("<tr>\n");
        result.append("    <td>").append(id).append("</td>\n");
        result.append("    <td>缺少: ").append(missingFields).append("</td>\n");
        
        if (errorMessage != null) {
            result.append("    <td class=\"error\">✗ 错误: ").append(errorMessage).append("</td>\n");
        } else if (updated) {
            result.append("    <td class=\"success\">✓ 已修复</td>\n");
        } else {
            result.append("    <td class=\"error\">✗ 无法从API获取完整数据</td>\n");
        }
        
        result.append("</tr>\n");
        return result.toString();
    }
    
    @GetMapping("/raw-api-data")
    public Mono<ResponseEntity<String>> getRawApiData(@RequestParam(defaultValue = "43573156") Long id) {
        logger.info("获取原始API数据，新闻ID: {}", id);
        
        return WebClient.builder()
                .build()
                .get()
                .uri("https://hacker-news.firebaseio.com/v0/item/{id}.json", id)
                .retrieve()
                .bodyToMono(String.class)
                .map(rawJson -> {
                    logger.info("获取到原始JSON数据: {}", rawJson);
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(rawJson);
                })
                .doOnError(e -> logger.error("获取原始数据出错: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just(
                    ResponseEntity.status(500)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("获取API数据失败: " + e.getMessage())
                ));
    }
    
    @GetMapping("/trace-processing")
    public Mono<ResponseEntity<String>> traceProcessing(@RequestParam(defaultValue = "43573156") Long id) {
        logger.info("开始追踪ID为{}的新闻处理过程", id);
        StringBuilder log = new StringBuilder();
        log.append("<!DOCTYPE html><html><head><title>处理跟踪</title>");
        log.append("<style>body{font-family:monospace;margin:20px;line-height:1.6;}");
        log.append(".step{margin-bottom:15px;padding:10px;border-left:4px solid #007bff;background:#f8f9fa;}");
        log.append(".json{background:#f5f5f5;padding:10px;overflow-x:auto;white-space:pre-wrap;font-size:12px;}");
        log.append(".success{color:green;}.error{color:red;}.warning{color:orange;}</style></head><body>");
        log.append("<h1>新闻项 #").append(id).append(" 处理跟踪</h1>");
        
        log.append("<div class='step'><h3>步骤 1: 从API获取原始数据</h3>");
        
        return WebClient.builder()
                .build()
                .get()
                .uri("https://hacker-news.firebaseio.com/v0/item/{id}.json", id)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(rawJson -> {
                    log.append("<p class='success'>✓ 成功获取API数据</p>");
                    log.append("<div class='json'>").append(rawJson).append("</div>");
                    
                    log.append("<div class='step'><h3>步骤 2: 解析JSON数据</h3>");
                    
                    try {
                        // 使用ObjectMapper解析JSON
                        HackerNewsService.HackerNewsItem item = objectMapper.readValue(rawJson, HackerNewsService.HackerNewsItem.class);
                        log.append("<p class='success'>✓ 成功解析JSON</p>");
                        log.append("<p>解析结果:</p>");
                        log.append("<ul>");
                        log.append("<li>ID: ").append(item.getId()).append("</li>");
                        log.append("<li>标题: ").append(item.getTitle()).append("</li>");
                        log.append("<li>URL: ").append(item.getUrl()).append("</li>");
                        log.append("<li>分数: ").append(item.getScore()).append("</li>");
                        log.append("<li>作者: ").append(item.getBy()).append("</li>");
                        log.append("<li>时间: ").append(item.getTime()).append("</li>");
                        log.append("<li>类型: ").append(item.getType()).append("</li>");
                        log.append("<li>评论数: ").append(item.getDescendants()).append("</li>");
                        log.append("</ul>");
                        
                        log.append("<div class='step'><h3>步骤 3: 检查数据库中是否已存在该记录</h3>");
                        boolean exists = repository.existsById(id);
                        NewsItem existingItem = null;
                        
                        if (exists) {
                            existingItem = repository.findById(id).orElse(null);
                            log.append("<p class='warning'>⚠ 数据库中已存在ID为").append(id).append("的记录</p>");
                            if (existingItem != null) {
                                log.append("<p>现有记录数据:</p>");
                                log.append("<ul>");
                                log.append("<li>标题(英): ").append(existingItem.getTitleEn()).append("</li>");
                                log.append("<li>标题(中): ").append(existingItem.getTitleZh()).append("</li>");
                                log.append("<li>排名: ").append(existingItem.getRank()).append("</li>");
                                log.append("<li>分数: ").append(existingItem.getScore()).append("</li>");
                                log.append("<li>最后更新: ").append(existingItem.getLastUpdated()).append("</li>");
                                log.append("</ul>");
                            }
                        } else {
                            log.append("<p class='success'>✓ 数据库中不存在该记录，将创建新记录</p>");
                        }
                        
                        log.append("<div class='step'><h3>步骤 4: 准备保存到数据库</h3>");
                        
                        // 创建或更新NewsItem对象
                        NewsItem newsItem;
                        if (existingItem != null) {
                            newsItem = existingItem;
                            newsItem.setScore(item.getScore());
                            newsItem.setLastUpdated(Instant.now());
                            log.append("<p class='success'>✓ 更新现有记录</p>");
                        } else {
                            newsItem = new NewsItem();
                            newsItem.setId(item.getId());
                            newsItem.setTitleEn(item.getTitle());
                            newsItem.setUrl(item.getUrl());
                            newsItem.setTextEn(item.getText());
                            newsItem.setType(item.getType());
                            newsItem.setTime(item.getTime());
                            newsItem.setCreatedAt(Instant.now());
                            newsItem.setLastUpdated(Instant.now());
                            newsItem.setScore(item.getScore());
                            newsItem.setRank(0); // 临时排名
                            log.append("<p class='success'>✓ 创建新记录</p>");
                        }
                        
                        log.append("<div class='step'><h3>步骤 5: 保存到数据库</h3>");
                        repository.save(newsItem);
                        log.append("<p class='success'>✓ 成功保存到数据库</p>");
                        
                        log.append("<div class='step'><h3>步骤 6: 最终数据库记录</h3>");
                        NewsItem finalItem = repository.findById(id).orElse(null);
                        if (finalItem != null) {
                            log.append("<p>最终保存的记录:</p>");
                            log.append("<ul>");
                            log.append("<li>ID: ").append(finalItem.getId()).append("</li>");
                            log.append("<li>标题(英): ").append(finalItem.getTitleEn()).append("</li>");
                            log.append("<li>标题(中): ").append(finalItem.getTitleZh()).append("</li>");
                            log.append("<li>URL: ").append(finalItem.getUrl()).append("</li>");
                            log.append("<li>排名: ").append(finalItem.getRank()).append("</li>");
                            log.append("<li>分数: ").append(finalItem.getScore()).append("</li>");
                            log.append("<li>创建时间: ").append(finalItem.getCreatedAt()).append("</li>");
                            log.append("<li>最后更新: ").append(finalItem.getLastUpdated()).append("</li>");
                            log.append("</ul>");
                        } else {
                            log.append("<p class='error'>✗ 无法从数据库获取最终记录</p>");
                        }
                        
                        log.append("</div></div></div></div></div>");
                        log.append("<p><a href='/api/test/dashboard'>返回测试面板</a></p>");
                        log.append("</body></html>");
                        
                        return Mono.just(ResponseEntity.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .body(log.toString()));
                        
                    } catch (Exception e) {
                        log.append("<p class='error'>✗ 解析JSON失败: ").append(e.getMessage()).append("</p>");
                        log.append("<div class='step'><h3>错误详情</h3>");
                        log.append("<p>").append(e.toString()).append("</p>");
                        log.append("<pre>");
                        for (StackTraceElement element : e.getStackTrace()) {
                            log.append(element.toString()).append("\n");
                        }
                        log.append("</pre></div>");
                        log.append("</body></html>");
                        
                        return Mono.just(ResponseEntity.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .body(log.toString()));
                    }
                })
                .onErrorResume(e -> {
                    log.append("<p class='error'>✗ 获取API数据失败: ").append(e.getMessage()).append("</p>");
                    log.append("</div></body></html>");
                    
                    return Mono.just(ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(log.toString()));
                });
    }
    
    @GetMapping("/raw-top-stories")
    public Mono<String> getRawTopStories(@RequestParam(defaultValue = "30") int limit) {
        logger.info("获取原始顶部故事列表，限制: {}", limit);
        
        return WebClient.builder()
                .build()
                .get()
                .uri("https://hacker-news.firebaseio.com/v0/topstories.json")
                .retrieve()
                .bodyToMono(String.class);
    }
    
    @GetMapping("/raw-batch-data")
    public Mono<String> getRawBatchData(@RequestParam(defaultValue = "10") int limit) {
        logger.info("批量获取前{}条新闻的原始数据", limit);
        
        return WebClient.builder()
                .build()
                .get()
                .uri("https://hacker-news.firebaseio.com/v0/topstories.json")
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(idsJson -> {
                    // 将JSON字符串解析为Long数组
                    try {
                        Long[] ids = objectMapper.readValue(idsJson, Long[].class);
                        int actualLimit = Math.min(ids.length, limit);
                        Long[] limitedIds = new Long[actualLimit];
                        System.arraycopy(ids, 0, limitedIds, 0, actualLimit);
                        
                        // 构建批量返回的JSON
                        StringBuilder result = new StringBuilder();
                        result.append("{\n");
                        result.append("  \"topStoryIds\": ").append(idsJson).append(",\n");
                        result.append("  \"batchData\": {\n");
                        
                        // 创建对每个ID的请求
                        List<Mono<String>> requests = new ArrayList<>();
                        for (int i = 0; i < limitedIds.length; i++) {
                            Long id = limitedIds[i];
                            final int index = i;
                            
                            Mono<String> request = WebClient.builder()
                                    .build()
                                    .get()
                                    .uri("https://hacker-news.firebaseio.com/v0/item/{id}.json", id)
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .map(itemJson -> {
                                        StringBuilder entry = new StringBuilder();
                                        entry.append("    \"").append(id).append("\": ");
                                        entry.append(itemJson);
                                        if (index < limitedIds.length - 1) {
                                            entry.append(",");
                                        }
                                        entry.append("\n");
                                        return entry.toString();
                                    });
                            
                            requests.add(request);
                        }
                        
                        // 将所有请求合并
                        return Flux.concat(requests)
                                .collectList()
                                .map(responses -> {
                                    for (String response : responses) {
                                        result.append(response);
                                    }
                                    result.append("  }\n");
                                    result.append("}");
                                    return result.toString();
                                });
                        
                    } catch (Exception e) {
                        logger.error("解析JSON出错: {}", e.getMessage());
                        return Mono.just("{ \"error\": \"" + e.getMessage() + "\" }");
                    }
                });
    }
    
    /**
     * 批量获取和更新新闻数据，使用改进的批处理策略
     */
    @GetMapping("/batch-fetch")
    public Mono<ResponseEntity<String>> batchFetch(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "10") int batchSize,
            @RequestParam(defaultValue = "3") int concurrency) {
        
        logger.info("触发批量获取更新，参数：limit={}, batchSize={}, concurrency={}", 
                    limit, batchSize, concurrency);
        
        // 验证和修正参数
        limit = Math.min(Math.max(limit, 1), 100); // 确保limit在1-100之间
        batchSize = Math.min(Math.max(batchSize, 1), 20); // 确保batchSize在1-20之间
        concurrency = Math.min(Math.max(concurrency, 1), 5); // 确保concurrency在1-5之间
        
        final int finalLimit = limit;
        final int finalBatchSize = batchSize;
        final int finalConcurrency = concurrency;
        
        return hackerNewsService.getTopStories(limit)
            .flatMap(topStoryIds -> {
                // 准备HTML结果页
                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html>\n");
                html.append("<html lang=\"zh-CN\">\n");
                html.append("<head>\n");
                html.append("    <meta charset=\"UTF-8\">\n");
                html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
                html.append("    <title>批量获取更新 - 结果</title>\n");
                html.append("    <style>\n");
                html.append("        body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; line-height: 1.6; max-width: 900px; margin: 0 auto; padding: 20px; }\n");
                html.append("        h1 { color: #ff6600; }\n");
                html.append("        .card { background-color: #f9f9f9; border-radius: 5px; padding: 15px; margin-bottom: 20px; }\n");
                html.append("        .info { color: #0077cc; }\n");
                html.append("        .success { color: #28a745; }\n");
                html.append("        .warning { color: #ffc107; }\n");
                html.append("        .error { color: #dc3545; }\n");
                html.append("        .progress-container { margin: 20px 0; }\n");
                html.append("        .progress-bar { background-color: #f0f0f0; border-radius: 4px; height: 20px; }\n");
                html.append("        .progress-fill { background-color: #4caf50; height: 100%; border-radius: 4px; transition: width 0.3s; }\n");
                html.append("        .back-link { margin-top: 20px; }\n");
                html.append("        pre { background-color: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto; }\n");
                html.append("        code { font-family: monospace; }\n");
                html.append("    </style>\n");
                html.append("</head>\n");
                html.append("<body>\n");
                html.append("    <h1>批量获取更新 - 进行中</h1>\n");
                
                // 显示配置信息
                html.append("    <div class=\"card\">\n");
                html.append("        <h2>配置参数</h2>\n");
                html.append("        <p>获取数量: <strong>").append(finalLimit).append("</strong></p>\n");
                html.append("        <p>批次大小: <strong>").append(finalBatchSize).append("</strong></p>\n");
                html.append("        <p>并发数量: <strong>").append(finalConcurrency).append("</strong></p>\n");
                html.append("    </div>\n");
                
                // 显示获取到的故事ID列表
                html.append("    <div class=\"card\">\n");
                html.append("        <h2>获取到的故事ID列表</h2>\n");
                html.append("        <p>共获取 <strong>").append(topStoryIds.size()).append("</strong> 个故事ID</p>\n");
                html.append("        <pre><code>").append(topStoryIds.toString()).append("</code></pre>\n");
                html.append("    </div>\n");
                
                // 准备批处理
                html.append("    <div class=\"card\">\n");
                html.append("        <h2>开始批处理</h2>\n");
                html.append("        <p class=\"info\">正在将 ").append(topStoryIds.size())
                      .append(" 个ID分成大小为 ").append(finalBatchSize).append(" 的批次进行处理...</p>\n");
                html.append("    </div>\n");
                
                // 将ID列表分成批次
                List<List<Long>> batches = new ArrayList<>();
                for (int i = 0; i < topStoryIds.size(); i += finalBatchSize) {
                    int end = Math.min(i + finalBatchSize, topStoryIds.size());
                    batches.add(topStoryIds.subList(i, end));
                }
                
                html.append("    <div class=\"card\">\n");
                html.append("        <h2>批处理结果</h2>\n");
                html.append("        <p>共分成 <strong>").append(batches.size()).append("</strong> 个批次</p>\n");
                
                // 创建排名映射
                Map<Long, Integer> rankMap = new HashMap<>();
                for (int i = 0; i < topStoryIds.size(); i++) {
                    rankMap.put(topStoryIds.get(i), i + 1);
                }
                
                // 开始处理批次
                // 这里使用 Flux.fromIterable(batches).index() 来处理批次
                // 但为了保持响应速度，我们不会等待所有批次完成，而是立即返回结果页面
                
                // 开始异步处理
                processNewsItemBatches(batches, rankMap, finalConcurrency);
                
                html.append("        <p class=\"success\">✓ 批处理已启动，正在后台进行</p>\n");
                html.append("        <p>结果将在后台处理，您可以通过首页查看更新后的新闻列表</p>\n");
                html.append("    </div>\n");
                
                // 添加链接
                html.append("    <div class=\"back-link\">\n");
                html.append("        <a href=\"/api/test/dashboard\">返回控制面板</a> | \n");
                html.append("        <a href=\"/\">查看首页</a> | \n");
                html.append("        <a href=\"/api/test/view-all-news\">查看所有新闻</a>\n");
                html.append("    </div>\n");
                
                html.append("</body>\n");
                html.append("</html>\n");
                
                return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html.toString()));
            })
            .onErrorResume(e -> {
                logger.error("批量获取过程中出错: {}", e.getMessage(), e);
                
                // 创建错误HTML页面
                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html>\n");
                html.append("<html lang=\"zh-CN\">\n");
                html.append("<head>\n");
                html.append("    <meta charset=\"UTF-8\">\n");
                html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
                html.append("    <title>批量获取更新 - 错误</title>\n");
                html.append("    <style>\n");
                html.append("        body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; line-height: 1.6; max-width: 900px; margin: 0 auto; padding: 20px; }\n");
                html.append("        h1 { color: #ff6600; }\n");
                html.append("        .error { color: #dc3545; }\n");
                html.append("        .back-link { margin-top: 20px; }\n");
                html.append("        pre { background-color: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto; }\n");
                html.append("    </style>\n");
                html.append("</head>\n");
                html.append("<body>\n");
                html.append("    <h1>批量获取更新 - 错误</h1>\n");
                html.append("    <div class=\"error\">\n");
                html.append("        <p>✗ 处理过程中出错:</p>\n");
                html.append("        <pre>").append(e.getMessage()).append("</pre>\n");
                html.append("    </div>\n");
                html.append("    <div class=\"back-link\">\n");
                html.append("        <a href=\"/api/test/dashboard\">返回控制面板</a>\n");
                html.append("    </div>\n");
                html.append("</body>\n");
                html.append("</html>\n");
                
                return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html.toString()));
            });
    }
    
    /**
     * 处理新闻项批次
     * 此方法将异步执行，不会阻塞响应
     */
    private void processNewsItemBatches(List<List<Long>> batches, Map<Long, Integer> rankMap, int concurrency) {
        Flux.fromIterable(batches)
            .index()
            .concatMap(tuple -> {
                Long batchIndex = tuple.getT1();
                List<Long> batchIds = tuple.getT2();
                
                logger.info("开始处理第 {} 批次，包含 {} 个ID", batchIndex + 1, batchIds.size());
                
                // 添加延迟，避免请求过于密集
                return Mono.delay(Duration.ofSeconds(batchIndex * 2))
                    .then(processBatch(batchIds, rankMap, concurrency));
            })
            .collectList()
            .doOnNext(results -> {
                logger.info("所有批次处理完成，共处理 {} 个批次", results.size());
            })
            .subscribe();
    }
    
    /**
     * 处理单个批次
     */
    private Mono<List<String>> processBatch(List<Long> batchIds, Map<Long, Integer> rankMap, int concurrency) {
        // 处理批次内的每个ID，限制并发数
        return Flux.fromIterable(batchIds)
            .flatMap(id -> {
                int rank = rankMap.getOrDefault(id, 999);
                return Mono.defer(() -> {
                    logger.info("处理ID: {}, 排名: {}", id, rank);
                    
                    return hackerNewsService.getItemById(id)
                        .flatMap(newsItem -> {
                            // 检查数据库中是否已存在该新闻
                            NewsItem dbItem;
                            boolean isNew = !repository.existsById(id);
                            
                            if (isNew) {
                                dbItem = new NewsItem();
                                dbItem.setId(id);
                                dbItem.setCreatedAt(Instant.now());
                            } else {
                                dbItem = repository.findById(id).orElse(new NewsItem());
                            }
                            
                            // 更新字段
                            if (newsItem.getTitle() != null) {
                                dbItem.setTitleEn(newsItem.getTitle());
                            }
                            
                            if (newsItem.getUrl() != null) {
                                dbItem.setUrl(newsItem.getUrl());
                            }
                            
                            if (newsItem.getText() != null) {
                                dbItem.setTextEn(newsItem.getText());
                            }
                            
                            if (newsItem.getTime() != null) {
                                dbItem.setTime(newsItem.getTime());
                            }
                            
                            if (newsItem.getType() != null) {
                                dbItem.setType(newsItem.getType());
                            }
                            
                            if (newsItem.getScore() != null) {
                                dbItem.setScore(newsItem.getScore());
                            }
                            
                            // 设置排名
                            dbItem.setRank(rank);
                            
                            // 更新最后更新时间
                            dbItem.setLastUpdated(Instant.now());
                            
                            // 保存到数据库
                            try {
                                repository.save(dbItem);
                                logger.info("成功保存ID: {}, isNew: {}", id, isNew);
                                return Mono.just("成功: " + id);
                            } catch (Exception e) {
                                logger.error("保存ID: {} 时出错: {}", id, e.getMessage());
                                return Mono.error(e);
                            }
                        });
                })
                .onErrorResume(e -> {
                    logger.error("处理ID: {} 时出错: {}", id, e.getMessage());
                    return Mono.empty();
                });
            }, concurrency)
            .collectList();
    }

    /**
     * 查看所有新闻记录
     * 显示数据库中所有新闻数据，包括完整和不完整的记录
     */
    @GetMapping("/view-all-news")
    public ResponseEntity<String> viewAllNews() {
        logger.info("查看所有新闻记录");
        
        // 获取所有新闻项
        List<NewsItem> allItems = repository.findAll(Sort.by(Sort.Direction.DESC, "time"));
        
        // 准备HTML结果页
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>所有新闻记录</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; line-height: 1.6; max-width: 1200px; margin: 0 auto; padding: 20px; }\n");
        html.append("        h1 { color: #ff6600; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append("        th, td { padding: 8px; border: 1px solid #ddd; text-align: left; font-size: 14px; }\n");
        html.append("        th { background-color: #f5f5f5; position: sticky; top: 0; }\n");
        html.append("        tr:nth-child(even) { background-color: #f9f9f9; }\n");
        html.append("        .incomplete { background-color: #fff3cd; }\n");
        html.append("        .back-link { margin: 20px 0; }\n");
        html.append("        .summary { margin: 20px 0; padding: 15px; background-color: #f8f9fa; border-radius: 5px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <h1>所有新闻记录</h1>\n");
        
        html.append("    <div class=\"summary\">\n");
        html.append("        <p>数据库中共有 ").append(allItems.size()).append(" 条新闻记录。</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"back-link\">\n");
        html.append("        <a href=\"/api/test/dashboard\">返回控制面板</a>\n");
        html.append("    </div>\n");
        
        // 创建结果表格
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th>ID</th>\n");
        html.append("            <th>排名</th>\n");
        html.append("            <th>分数</th>\n");
        html.append("            <th>原标题</th>\n");
        html.append("            <th>翻译标题</th>\n");
        html.append("            <th>类型</th>\n");
        html.append("            <th>创建时间</th>\n");
        html.append("            <th>最后更新</th>\n");
        html.append("            <th>链接</th>\n");
        html.append("        </tr>\n");
        
        // 添加所有新闻项
        for (NewsItem item : allItems) {
            boolean isIncomplete = item.getTitleEn() == null || 
                                  item.getRank() == null || 
                                  item.getScore() == null;
            
            html.append("        <tr").append(isIncomplete ? " class=\"incomplete\"" : "").append(">\n");
            html.append("            <td>").append(item.getId()).append("</td>\n");
            html.append("            <td>").append(item.getRank() != null ? item.getRank() : "缺失").append("</td>\n");
            html.append("            <td>").append(item.getScore() != null ? item.getScore() : "缺失").append("</td>\n");
            html.append("            <td>").append(item.getTitleEn() != null ? item.getTitleEn() : "缺失").append("</td>\n");
            html.append("            <td>").append(item.getTitleZh() != null ? item.getTitleZh() : "缺失").append("</td>\n");
            html.append("            <td>").append(item.getType() != null ? item.getType() : "未知").append("</td>\n");
            html.append("            <td>").append(item.getCreatedAt() != null ? item.getCreatedAt() : "未知").append("</td>\n");
            html.append("            <td>").append(item.getLastUpdated() != null ? item.getLastUpdated() : "未知").append("</td>\n");
            
            // 添加链接
            if (item.getUrl() != null) {
                html.append("            <td><a href=\"").append(item.getUrl()).append("\" target=\"_blank\">原文</a></td>\n");
            } else {
                html.append("            <td>无链接</td>\n");
            }
            
            html.append("        </tr>\n");
        }
        
        html.append("    </table>\n");
        
        html.append("    <div class=\"back-link\">\n");
        html.append("        <a href=\"/api/test/dashboard\">返回控制面板</a>\n");
        html.append("    </div>\n");
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html.toString());
    }

    /**
     * 比较翻译结果测试端点
     * 用法示例: /api/test/compare-translation?text=Hello%20World
     */
    @GetMapping(value = "/compare-translation", produces = "text/html")
    public Mono<String> compareTranslation(@RequestParam(defaultValue = "Hello, this is a test for comparing translation quality between Baidu and DeepL.") String text) {
        logger.info("比较翻译测试: {}", text);
        
        // 临时禁用DeepL，使用百度翻译
        boolean originalUseDeepL = true;
        try {
            java.lang.reflect.Field field = translationService.getClass().getDeclaredField("useDeepL");
            field.setAccessible(true);
            originalUseDeepL = field.getBoolean(translationService);
            field.setBoolean(translationService, false);
        } catch (Exception e) {
            logger.error("获取或设置useDeepL字段失败", e);
        }
        
        // 使用百度翻译
        Mono<String> baiduResult = translationService.translateEnToZh(text);
        
        // 恢复并启用DeepL翻译
        try {
            java.lang.reflect.Field field = translationService.getClass().getDeclaredField("useDeepL");
            field.setAccessible(true);
            field.setBoolean(translationService, true);
        } catch (Exception e) {
            logger.error("设置useDeepL字段失败", e);
        }
        
        // 使用DeepL翻译
        Mono<String> deeplResult = translationService.translateEnToZh(text);
        
        // 恢复原始设置
        try {
            java.lang.reflect.Field field = translationService.getClass().getDeclaredField("useDeepL");
            field.setAccessible(true);
            field.setBoolean(translationService, originalUseDeepL);
        } catch (Exception e) {
            logger.error("恢复useDeepL字段失败", e);
        }
        
        // 组合结果并生成HTML页面
        return Mono.zip(baiduResult, deeplResult)
                .map(tuple -> {
                    String baiduTranslation = tuple.getT1();
                    String deeplTranslation = tuple.getT2();
                    
                    StringBuilder html = new StringBuilder();
                    html.append("<!DOCTYPE html><html><head><title>翻译比较测试</title>");
                    html.append("<meta charset=\"UTF-8\"><style>");
                    html.append("body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}");
                    html.append(".card{border:1px solid #ddd;border-radius:8px;padding:15px;margin-top:20px;}");
                    html.append(".original{color:#333;font-weight:bold;}.baidu{color:#c05b4d;}.deepl{color:#0066cc;font-weight:bold;}");
                    html.append("h2{margin-top:30px;color:#444;border-bottom:1px solid #eee;padding-bottom:10px;}");
                    html.append(".comparison{display:flex;gap:20px;margin-top:20px;}");
                    html.append(".comparison > div{flex:1;padding:15px;border-radius:8px;}");
                    html.append(".baidu-box{background-color:#fff1f0;border:1px solid #ffccc7;}");
                    html.append(".deepl-box{background-color:#e6f7ff;border:1px solid #91d5ff;}");
                    html.append("</style></head><body>");
                    html.append("<h1>翻译服务比较</h1>");
                    html.append("<div class=\"card\">");
                    html.append("<p>原文: <span class=\"original\">").append(text).append("</span></p>");
                    
                    html.append("<h2>翻译结果比较</h2>");
                    html.append("<div class=\"comparison\">");
                    
                    html.append("<div class=\"baidu-box\">");
                    html.append("<h3>百度翻译</h3>");
                    html.append("<p class=\"baidu\">").append(baiduTranslation).append("</p>");
                    html.append("</div>");
                    
                    html.append("<div class=\"deepl-box\">");
                    html.append("<h3>DeepL翻译</h3>");
                    html.append("<p class=\"deepl\">").append(deeplTranslation).append("</p>");
                    html.append("</div>");
                    
                    html.append("</div>"); // 结束comparison
                    
                    html.append("</div>"); // 结束card
                    html.append("<p style=\"margin-top:20px;\">提示: 修改URL中的text参数可翻译不同文本</p>");
                    html.append("<p><a href=\"/api/test/dashboard\">返回测试控制面板</a></p>");
                    html.append("</body></html>");
                    return html.toString();
                });
    }

    /**
     * 彻底重置并更新，清空数据库和缓存
     * 用法示例: /api/test/complete-reset
     */
    @GetMapping("/complete-reset")
    public Mono<ResponseEntity<String>> completeReset() {
        logger.info("彻底重置系统 - 清空数据库和缓存");
        
        try {
            // 清空数据库
            repository.deleteAll();
            logger.info("数据库已清空");
            
            // 清空缓存
            int cacheKeysCleared = cacheService.clearAllCache();
            logger.info("缓存已清空，共清除{}个缓存项", cacheKeysCleared);
            
            // 触发新闻更新
            newsUpdateService.updateNews();
            logger.info("已触发新闻更新");
            
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<!DOCTYPE html>\n<html>\n<head>\n" +
                          "<meta charset=\"UTF-8\">\n" +
                          "<title>彻底重置系统</title>\n" +
                          "<style>body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}</style>\n" +
                          "</head>\n<body>\n" +
                          "<h1>系统已彻底重置</h1>" +
                          "<p>✓ 数据库已清空</p>" +
                          "<p>✓ 缓存已清空，共清除" + cacheKeysCleared + "个缓存项</p>" +
                          "<p>✓ 已触发新闻更新</p>" +
                          "<p>请等待几分钟，然后<a href='/'>刷新首页</a>查看结果</p>" +
                          "<p><a href='/api/test/dashboard'>返回测试面板</a></p>" +
                          "</body>\n</html>"));
        } catch (Exception e) {
            logger.error("彻底重置和更新过程中出错: {}", e.getMessage(), e);
            
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<!DOCTYPE html>\n<html>\n<head>\n" +
                          "<meta charset=\"UTF-8\">\n" +
                          "<title>操作失败</title>\n" +
                          "<style>body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}</style>\n" +
                          "</head>\n<body>\n" +
                          "<h1>操作失败</h1>" +
                          "<p>彻底重置系统时出错: " + e.getMessage() + "</p>" +
                          "<p><a href='/api/test/dashboard'>返回测试面板</a></p>" +
                          "</body>\n</html>"));
        }
    }
} 