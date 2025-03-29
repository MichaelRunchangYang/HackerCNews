package com.example.hacker_cnews.service;

import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TranslationService {
    private final WebClient webClient;
    
    @Value("${baidu.translate.appid}")
    private String appId;
    
    @Value("${baidu.translate.key}")
    private String securityKey;
    
    private static final String TRANSLATE_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    
    public TranslationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    public Mono<String> translateEnToZh(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Mono.just("");
        }
        
        // 生成随机数
        String salt = String.valueOf(System.currentTimeMillis());
        // 拼接签名原文
        String sign = appId + text + salt + securityKey;
        // 计算MD5签名
        String md5Sign = md5(sign);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(TRANSLATE_API_URL)
                        .queryParam("q", text)
                        .queryParam("from", "en")
                        .queryParam("to", "zh")
                        .queryParam("appid", appId)
                        .queryParam("salt", salt)
                        .queryParam("sign", md5Sign)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    // 解析翻译结果
                    List<Map<String, String>> transResult = (List<Map<String, String>>) response.get("trans_result");
                    if (transResult != null && !transResult.isEmpty()) {
                        return transResult.get(0).get("dst");
                    }
                    return "";
                })
                .onErrorReturn("");  // 出错时返回空字符串
    }
    
    // MD5加密方法
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
} 