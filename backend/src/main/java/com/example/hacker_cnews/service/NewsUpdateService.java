package com.example.hacker_cnews.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.hacker_cnews.config.HackerNewsConfig;
import com.example.hacker_cnews.entity.NewsItem;
import com.example.hacker_cnews.repository.NewsItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class NewsUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(NewsUpdateService.class);
    
    private final HackerNewsService hackerNewsService;
    private final TranslationService translationService;
    private final NewsItemRepository repository;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final HackerNewsConfig hackerNewsConfig;
    
    // 用于跟踪处理失败的ID
    private final Set<Long> failedIds = ConcurrentHashMap.newKeySet();
    
    // 批处理配置
    private static final int BATCH_SIZE = 10; // 每批处理的ID数量
    private static final int BATCH_DELAY_SECONDS = 2; // 批次间延迟秒数
    private static final int BATCH_CONCURRENCY = 3; // 每批内的并发请求数
    
    public NewsUpdateService(
            HackerNewsService hackerNewsService,
            TranslationService translationService,
            NewsItemRepository repository,
            CacheService cacheService,
            ObjectMapper objectMapper,
            HackerNewsConfig hackerNewsConfig) {
        this.hackerNewsService = hackerNewsService;
        this.translationService = translationService;
        this.repository = repository;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.hackerNewsConfig = hackerNewsConfig;
    }
    
    @Scheduled(fixedDelayString = "${hacker-news.poll.interval}")
    public void updateNews() {
        logger.info("开始更新新闻数据...");
        
        // 清空失败ID列表
        failedIds.clear();
        
        // 获取ID列表并进行分批处理
        hackerNewsService.getTopStories(hackerNewsConfig.getItemsLimit())
                .flatMap(allIds -> {
                    logger.info("获取到 {} 条热门新闻ID，准备分批处理", allIds.size());
                    
                    // 创建ID到排名的映射
                    Map<Long, Integer> rankMap = new HashMap<>();
                    for (int i = 0; i < allIds.size(); i++) {
                        rankMap.put(allIds.get(i), i + 1);
                    }
                    
                    // 将ID列表分成小批次
                    List<List<Long>> batches = new ArrayList<>();
                    for (int i = 0; i < allIds.size(); i += BATCH_SIZE) {
                        int end = Math.min(i + BATCH_SIZE, allIds.size());
                        batches.add(allIds.subList(i, end));
                    }
                    
                    logger.info("将 {} 个ID分为 {} 个批次处理，每批 {} 个ID", 
                        allIds.size(), batches.size(), BATCH_SIZE);
                    
                    // 顺序处理每个批次，批次之间添加延迟
                    return Flux.fromIterable(batches)
                        .index()  // 添加批次索引
                        .concatMap(tuple -> {
                            Long batchIndex = tuple.getT1();
                            List<Long> batchIds = tuple.getT2();
                            
                            logger.info("开始处理第 {} 批次，包含 {} 个ID", batchIndex + 1, batchIds.size());
                            
                            // 添加延迟，避免请求过于密集
                            return Mono.delay(Duration.ofSeconds(batchIndex * BATCH_DELAY_SECONDS))
                                .then(processBatch(batchIds, rankMap));
                        })
                        .collectList()
                        .doOnNext(batchResults -> {
                            logger.info("所有批次处理完成，总计处理 {} 个批次", batchResults.size());
                            
                            // 如果有失败的ID，尝试重新处理
                            if (!failedIds.isEmpty()) {
                                logger.info("有 {} 个ID处理失败，将在后续尝试重新处理", failedIds.size());
                                scheduleRetryFailedIds(rankMap);
                            }
                            
                            // 清理过期新闻
                            cleanupOldNews();
                        });
                })
                .subscribe();
    }
    
    /**
     * 处理一个批次的ID
     */
    private Mono<List<NewsItem>> processBatch(List<Long> batchIds, Map<Long, Integer> rankMap) {
        // 处理批次内的每个ID，限制并发数
        return Flux.fromIterable(batchIds)
            .flatMap(id -> {
                int rank = rankMap.getOrDefault(id, 999); // 使用映射中的排名
                return processNewsItem(id, rank)
                    .doOnError(e -> {
                        // 记录失败的ID，便于后续重试
                        failedIds.add(id);
                        logger.error("处理ID{}时出错: {}", id, e.getMessage());
                    })
                    .onErrorResume(e -> Mono.empty()); // 继续处理其他ID
            }, BATCH_CONCURRENCY) // 限制并发请求数
            .collectList()
            .doOnNext(results -> {
                int successCount = results.size();
                int failureCount = batchIds.size() - successCount;
                logger.info("批次处理完成: 成功={}, 失败={}", successCount, failureCount);
            });
    }
    
    /**
     * 安排重试失败的ID
     */
    private void scheduleRetryFailedIds(Map<Long, Integer> rankMap) {
        // 复制失败ID列表，避免并发修改
        List<Long> idsToRetry = new ArrayList<>(failedIds);
        failedIds.clear(); // 清空原列表
        
        logger.info("开始重试 {} 个失败的ID", idsToRetry.size());
        
        // 将失败ID分成更小的批次，增加间隔，减小并发
        List<List<Long>> retryBatches = new ArrayList<>();
        for (int i = 0; i < idsToRetry.size(); i += 5) { // 每批5个，比正常批次更小
            int end = Math.min(i + 5, idsToRetry.size());
            retryBatches.add(idsToRetry.subList(i, end));
        }
        
        // 重试失败的ID
        Flux.fromIterable(retryBatches)
            .index()
            .concatMap(tuple -> {
                Long batchIndex = tuple.getT1();
                List<Long> batchIds = tuple.getT2();
                
                // 增加更长的延迟
                return Mono.delay(Duration.ofSeconds(batchIndex * 3 + 5))
                    .then(Mono.defer(() -> {
                        logger.info("重试批次 {}: {}", batchIndex + 1, batchIds);
                        return processBatch(batchIds, rankMap);
                    }));
            })
            .collectList()
            .subscribe(
                results -> logger.info("重试完成，处理了 {} 个批次", results.size()),
                error -> logger.error("重试过程中出错: {}", error.getMessage())
            );
    }
    
    /**
     * 清理过期新闻，保留最新的N条（按更新时间和排名排序）
     */
    private void cleanupOldNews() {
        long count = repository.count();
        int maxItems = hackerNewsConfig.getMaxStoredItems();
        
        if (count > maxItems) {
            logger.info("新闻数量超过最大限制，开始清理...");
            
            // 修改排序逻辑：优先按最后更新时间降序排序，其次按排名升序排序
            List<NewsItem> allNews = repository.findAll(
                Sort.by(Sort.Direction.DESC, "lastUpdated")
                    .and(Sort.by(Sort.Direction.ASC, "rank"))
            );
            
            // 保留前maxItems条，删除其余
            if (allNews.size() > maxItems) {
                List<NewsItem> newsToDelete = allNews.subList(maxItems, allNews.size());
                List<Long> idsToDelete = new ArrayList<>();
                
                for (NewsItem item : newsToDelete) {
                    idsToDelete.add(item.getId());
                }
                
                if (!idsToDelete.isEmpty()) {
                    logger.info("删除 {} 条旧新闻", idsToDelete.size());
                    repository.deleteAllById(idsToDelete);
                }
            }
        }
    }
    
    /**
     * 处理单个新闻项
     * 增强了数据完整性验证
     */
    private Mono<NewsItem> processNewsItem(Long id, int rank) {
        return hackerNewsService.getItemById(id)
                .flatMap(hnItem -> {
                    // 检查ID是否有效
                    if (hnItem == null || hnItem.getId() == null) {
                        logger.error("Hacker News项目ID无效: {}", id);
                        return Mono.empty();
                    }
                    
                    // 检查关键数据是否完整
                    if (hnItem.getTitle() == null) {
                        logger.error("Hacker News项目标题为空，跳过处理: ID={}", id);
                        return Mono.empty();
                    }
                    
                    // 检查数据库是否已存在
                    NewsItem existingItem = null;
                    if (repository.existsById(hnItem.getId())) {
                        logger.info("新闻已存在于数据库中: {}", hnItem.getId());
                        existingItem = repository.findById(hnItem.getId()).orElse(null);
                        
                        if (existingItem != null) {
                            boolean updated = false;
                            
                            // 更新排名（如果不存在或有变化）
                            if (existingItem.getRank() == null || existingItem.getRank() != rank) {
                                existingItem.setRank(rank);
                                updated = true;
                            }
                            
                            // 更新分数（如果不存在或有变化）
                            if ((existingItem.getScore() == null || 
                                 (hnItem.getScore() != null && !existingItem.getScore().equals(hnItem.getScore()))) && 
                                hnItem.getScore() != null) {
                                existingItem.setScore(hnItem.getScore());
                                updated = true;
                            }
                            
                            // 如果是不完整的记录（缺少标题），但现在有了完整数据，则更新
                            if ((existingItem.getTitleEn() == null || existingItem.getTitleEn().trim().isEmpty()) && 
                                hnItem.getTitle() != null) {
                                existingItem.setTitleEn(hnItem.getTitle());
                                updated = true;
                            }
                            
                            // 如果是不完整的记录（缺少URL），但现在有了完整数据，则更新
                            if ((existingItem.getUrl() == null || existingItem.getUrl().trim().isEmpty()) && 
                                hnItem.getUrl() != null) {
                                existingItem.setUrl(hnItem.getUrl());
                                updated = true;
                            }
                            
                            // 如果有更新，更新最后更新时间
                            if (updated) {
                                existingItem.setLastUpdated(Instant.now());
                                // 修复：将同步save操作转换为Mono
                                NewsItem savedItem = repository.save(existingItem);
                                // 添加：在数据库更新后立即更新缓存
                                try {
                                    cacheService.cacheNewsItem(savedItem.getId(), savedItem);
                                    logger.info("更新数据库后，缓存也已更新: {}", savedItem.getId());
                                } catch (Exception e) {
                                     // 捕获可能的Redis序列化/连接错误
                                    logger.error("更新缓存时出错 ID: {}, 错误: {}", savedItem.getId(), e.getMessage());
                                }
                                return Mono.just(savedItem);
                            }
                            
                            return Mono.just(existingItem);
                        }
                    }
                    
                    logger.info("创建新的NewsItem: {}", hnItem.getId());
                    NewsItem newsItem = new NewsItem();
                    newsItem.setId(hnItem.getId());
                    newsItem.setTitleEn(hnItem.getTitle());
                    newsItem.setUrl(hnItem.getUrl());
                    newsItem.setTextEn(hnItem.getText());
                    newsItem.setType(hnItem.getType());
                    newsItem.setTime(hnItem.getTime());
                    newsItem.setCreatedAt(Instant.now());
                    newsItem.setLastUpdated(Instant.now());
                    newsItem.setRank(rank); // 设置排名
                    newsItem.setScore(hnItem.getScore()); // 设置分数
                    
                    try {
                        if (hnItem.getKids() != null) {
                            newsItem.setCommentIds(objectMapper.writeValueAsString(hnItem.getKids()));
                        }
                    } catch (Exception e) {
                        logger.error("序列化评论ID时出错: {}", e.getMessage());
                    }
                    
                    // 查找缓存中是否有翻译
                    String cachedTitle = cacheService.getCachedTranslation(hnItem.getTitle());
                    String cachedText = hnItem.getText() != null ? 
                            cacheService.getCachedTranslation(hnItem.getText()) : null;
                    
                    logger.info("开始翻译标题: {}", hnItem.getTitle());
                    Mono<String> titleTranslation = cachedTitle != null ? 
                            Mono.just(cachedTitle) : 
                            translationService.translateEnToZh(hnItem.getTitle())
                                .doOnError(e -> logger.error("翻译标题出错: {}", e.getMessage()));
                    
                    Mono<String> textTranslation = Mono.just("");
                    if (hnItem.getText() != null) {
                        logger.info("开始翻译正文");
                        textTranslation = cachedText != null ? 
                                Mono.just(cachedText) : 
                                translationService.translateEnToZh(hnItem.getText())
                                    .doOnError(e -> logger.error("翻译正文出错: {}", e.getMessage()));
                    }
                    
                    return Mono.zip(titleTranslation, textTranslation)
                            .map(tuple -> {
                                String translatedTitle = tuple.getT1();
                                String translatedText = tuple.getT2();
                                
                                logger.info("翻译完成, 标题: {}", translatedTitle);
                                
                                // 保存到缓存
                                if (cachedTitle == null) {
                                    cacheService.cacheTranslation(hnItem.getTitle(), translatedTitle);
                                }
                                
                                if (cachedText == null && hnItem.getText() != null) {
                                    cacheService.cacheTranslation(hnItem.getText(), translatedText);
                                }
                                
                                newsItem.setTitleZh(translatedTitle);
                                newsItem.setTextZh(translatedText);
                                
                                // 确保ID已设置
                                if (newsItem.getId() == null) {
                                    logger.error("新闻项ID为NULL，设置为原始ID: {}", hnItem.getId());
                                    newsItem.setId(hnItem.getId());
                                }
                                
                                // 缓存新闻项
                                cacheService.cacheNewsItem(newsItem.getId(), newsItem);
                                logger.info("新闻项已缓存: {}", newsItem.getId());
                                
                                // 保存到数据库并返回结果
                                NewsItem savedItem = repository.save(newsItem);
                                return savedItem;
                            });
                })
                .doOnSuccess(item -> {
                    if (item != null) {
                        logger.info("成功处理ID: {}, 排名: {}, 分数: {}", item.getId(), item.getRank(), item.getScore());
                    }
                })
                .doOnError(e -> {
                    logger.error("处理ID: {} 时出错: {}", id, e.getMessage());
                    failedIds.add(id); // 记录失败的ID
                })
                .onErrorResume(e -> Mono.empty());
    }
    
    /**
     * 修复数据库中不完整的记录
     * 可以通过API手动触发
     */
    public Mono<Void> fixIncompleteRecords() {
        logger.info("开始修复不完整的记录...");
        
        // 查找所有不完整的记录（没有标题或排名或分数为空）
        return Mono.fromCallable(() -> 
            repository.findAll().stream()
                .filter(item -> item.getTitleEn() == null || 
                              item.getTitleEn().trim().isEmpty() || 
                              item.getRank() == null || 
                              item.getScore() == null)
                .toList()
        )
        .flatMapMany(incompleteItems -> {
            logger.info("找到 {} 条不完整的记录", incompleteItems.size());
            
            if (incompleteItems.isEmpty()) {
                logger.info("没有需要修复的记录");
                return Flux.empty();
            }
            
            // 获取topstories列表，用于获取最新排名
            return hackerNewsService.getTopStories(100)
                .flatMapMany(topStoryIds -> {
                    // 创建ID到排名的映射
                    Map<Long, Integer> rankMap = new HashMap<>();
                    for (int i = 0; i < topStoryIds.size(); i++) {
                        rankMap.put(topStoryIds.get(i), i + 1);
                    }
                    
                    // 修复每条不完整的记录
                    return Flux.fromIterable(incompleteItems)
                        .flatMap(item -> {
                            // 使用ID映射中的排名，如果没有则保留原排名或设为默认值
                            int rank = rankMap.getOrDefault(item.getId(), 
                                item.getRank() != null ? item.getRank() : 999);
                            
                            logger.info("修复记录ID: {}, 排名: {}", item.getId(), rank);
                            return processNewsItem(item.getId(), rank)
                                .onErrorResume(e -> {
                                    logger.error("修复记录时发生错误 ID: {}, 错误: {}", item.getId(), e.getMessage());
                                    return Mono.empty();
                                });
                        }, 3); // 限制并发为3
                })
                .onErrorResume(e -> {
                    logger.error("获取最新排名时发生错误: {}", e.getMessage());
                    return Flux.empty();
                });
        })
        .then();
    }
} 