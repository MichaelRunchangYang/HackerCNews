package com.example.hacker_cnews.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class TranslationService {
    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
    private final WebClient webClient;
    
    @Value("${deepl.translate.key}")
    private String deeplApiKey;
    
    @Value("${deepl.translate.url}")
    private String deeplApiUrl;
    
    public TranslationService(WebClient.Builder webClientBuilder) {
        // 不设置baseUrl，让每个请求使用完整URL
        this.webClient = webClientBuilder.build();
    }
    
    public Mono<String> translateEnToZh(String text) {
        if (text == null || text.trim().isEmpty()) {
            logger.info("翻译请求：文本为空");
            return Mono.just("");
        }
        
        logger.info("翻译请求：{}", text);
        return translateWithDeepL(text);
    }
    
    /**
     * 使用DeepL API进行翻译
     */
    private Mono<String> translateWithDeepL(String text) {
        logger.debug("使用DeepL API进行翻译");
        
        // 添加截断机制，DeepL API也有字符限制
        final String truncatedText;
        if (text.length() > 5000) {  // DeepL免费API单次请求字符限制更高
            truncatedText = text.substring(0, 5000);
            logger.warn("文本过长，已截断至5000字符");
        } else {
            truncatedText = text;
        }
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", List.of(truncatedText));
            requestBody.put("source_lang", "EN");
            requestBody.put("target_lang", "ZH");
            
            // 发送POST请求到DeepL API
            return webClient.post()
                    .uri(deeplApiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "DeepL-Auth-Key " + deeplApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .doOnNext(response -> logger.debug("DeepL API响应: {}", response))
                    .map(response -> {
                        // 解析翻译结果
                        if (response.containsKey("translations")) {
                            List<Map<String, Object>> translations = (List<Map<String, Object>>) response.get("translations");
                            if (translations != null && !translations.isEmpty()) {
                                String result = (String) translations.get(0).get("text");
                                logger.info("DeepL翻译成功: {} -> {}", text, result);
                                return result;
                            }
                        }
                        logger.warn("DeepL翻译响应中没有发现结果");
                        return "无法获取翻译结果";
                    })
                    .doOnError(error -> logger.error("DeepL翻译过程中发生错误: {}", error.getMessage(), error))
                    .onErrorReturn("翻译服务暂时不可用，请稍后再试");
        } catch (Exception e) {
            logger.error("创建DeepL翻译请求时发生错误: {}", e.getMessage(), e);
            return Mono.just("翻译请求构建失败: " + e.getMessage());
        }
    }
} 