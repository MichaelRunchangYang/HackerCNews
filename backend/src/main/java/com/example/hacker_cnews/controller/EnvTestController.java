package com.example.hacker_cnews.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class EnvTestController {

    @Autowired
    private Environment env;
    
    @Value("${MYSQL_USERNAME:未设置}")
    private String mysqlUsername;
    
    @Value("${REDIS_HOST:未设置}")
    private String redisHost;
    
    @Value("${BAIDU_TRANSLATE_APPID:未设置}")
    private String baiduAppId;
    
    @GetMapping("/env")
    public Map<String, Object> testEnv() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取通过Environment的环境变量
        result.put("env_mysql_username", env.getProperty("MYSQL_USERNAME", "未通过env获取到"));
        result.put("env_redis_host", env.getProperty("REDIS_HOST", "未通过env获取到"));
        result.put("env_baidu_appid", env.getProperty("BAIDU_TRANSLATE_APPID", "未通过env获取到"));
        
        // 获取通过@Value注入的环境变量
        result.put("value_mysql_username", mysqlUsername);
        result.put("value_redis_host", redisHost);
        result.put("value_baidu_appid", baiduAppId);
        
        // 获取应用属性
        result.put("application_db_username", env.getProperty("spring.datasource.username", "未获取到"));
        result.put("application_redis_host", env.getProperty("spring.redis.host", "未获取到"));
        result.put("application_baidu_appid", env.getProperty("baidu.translate.appid", "未获取到"));
        
        return result;
    }
} 