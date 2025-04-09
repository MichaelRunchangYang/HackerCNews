package com.example.hacker_cnews.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class TranslationService {
    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
    private final WebClient webClient;
    
    @Value("${baidu.translate.appid}")
    private String appId;
    
    @Value("${baidu.translate.key}")
    private String securityKey;
    
    private static final String TRANSLATE_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    
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
        
        // 添加截断机制，百度翻译API对于长文本有限制
        final String truncatedText;
        if (text.length() > 2000) {
            truncatedText = text.substring(0, 2000);
            logger.warn("文本过长，已截断至2000字符");
        } else {
            truncatedText = text;
        }
        
        // 生成随机数
        final String salt = String.valueOf(System.currentTimeMillis());
        
        // 百度API要求：拼接签名原文
        // 签名生成方法：appid+q+salt+key，其中q为原始文本，不做URL编码
        String signStr = appId + truncatedText + salt + securityKey;
        logger.debug("签名字符串(appid+q+salt+key): {}", signStr);
        
        // 计算MD5签名
        final String md5Sign = md5(signStr);
        
        logger.debug("翻译请求参数: appid={}, salt={}, sign={}", appId, salt, md5Sign);
        
        try {
            // 在请求发送前对q进行URL编码（按照百度API文档要求）
            final String encodedText = URLEncoder.encode(truncatedText, StandardCharsets.UTF_8.name());
            logger.debug("URL编码后的文本: {}", encodedText);
            
            // 构建请求 - 使用POST方法（百度API推荐）
            return webClient.post()
                    .uri("https://fanyi-api.baidu.com/api/trans/vip/translate")
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body(org.springframework.web.reactive.function.BodyInserters.fromFormData("q", truncatedText)
                            .with("from", "en")
                            .with("to", "zh")
                            .with("appid", appId)
                            .with("salt", salt)
                            .with("sign", md5Sign))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .doOnNext(response -> {
                        logger.info("翻译API响应: {}", response);
                        if (response != null && response.containsKey("trans_result")) {
                            List<Map<String, String>> results = (List<Map<String, String>>) response.get("trans_result");
                            if (results != null && !results.isEmpty()) {
                                logger.info("翻译返回源文本: {}", results.get(0).get("src"));
                            }
                        }
                    })
                    .map(response -> {
                        // 检查是否有错误
                        if (response.containsKey("error_code")) {
                            String errorCode = response.get("error_code").toString();
                            String errorMsg = response.containsKey("error_msg") ? 
                                              response.get("error_msg").toString() : "未知错误";
                            logger.error("翻译API错误: 错误码={}, 错误消息={}", errorCode, errorMsg);
                            
                            // 根据百度翻译API文档解释错误码
                            String explanation = getErrorExplanation(errorCode);
                            logger.error("错误说明: {}", explanation);
                            
                            return "翻译出错: " + errorMsg + " (" + explanation + ")";
                        }
                        
                        // 解析翻译结果
                        List<Map<String, String>> transResult = (List<Map<String, String>>) response.get("trans_result");
                        if (transResult != null && !transResult.isEmpty()) {
                            String result = transResult.get(0).get("dst");
                            logger.info("翻译成功: {} -> {}", text, result);
                            return result;
                        }
                        logger.warn("翻译响应中没有发现结果");
                        return "无法获取翻译结果";
                    })
                    .doOnError(error -> logger.error("翻译过程中发生错误: {}", error.getMessage(), error))
                    .onErrorReturn("翻译服务暂时不可用，请稍后再试");
        } catch (Exception e) {
            logger.error("创建翻译请求时发生错误: {}", e.getMessage(), e);
            return Mono.just("翻译请求构建失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据百度翻译API文档解释错误码
     */
    private String getErrorExplanation(String errorCode) {
        switch (errorCode) {
            case "52000": return "成功";
            case "52001": return "请求超时，请检查文本长度或网络";
            case "52002": return "系统错误";
            case "52003": return "未授权用户，检查appid或开通服务";
            case "54000": return "必填参数为空";
            case "54001": return "签名错误，请检查签名生成方法";
            case "54003": return "访问频率受限，请降低调用频率或升级账户";
            case "54004": return "账户余额不足";
            case "54005": return "长query请求频繁，请降低长文本发送频率";
            case "58000": return "客户端IP非法";
            case "58001": return "译文语言方向不支持";
            case "58002": return "服务当前已关闭";
            default: return "未知错误码";
        }
    }
    
    // MD5加密方法
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("MD5加密过程中发生错误: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
} 