package com.example.hacker_cnews.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class PropertiesConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(PropertiesConfig.class);
    
    // 敏感属性关键字列表，用于识别不应打印的属性
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "key", "token", "credentials", "pwd"
    );
    
    @Autowired
    private ConfigurableEnvironment env;
    
    @PostConstruct
    public void initializeProperties() {
        try {
            Resource resource = new ClassPathResource(".env.properties");
            Properties props = new Properties();
            props.load(resource.getInputStream());
            
            Map<String, Object> propertiesMap = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                propertiesMap.put(key, value);
                
                // 同时设置系统属性，以便在非Spring上下文中也能访问
                System.setProperty(key, value);
            }
            
            // 添加到Spring环境中
            MapPropertySource propertySource = new MapPropertySource("dotenvProperties", propertiesMap);
            env.getPropertySources().addFirst(propertySource);
            
            // 仅记录已加载的非敏感配置键名，不记录值
            Set<String> nonSensitiveKeys = props.stringPropertyNames().stream()
                    .filter(k -> SENSITIVE_KEYS.stream().noneMatch(k.toLowerCase()::contains))
                    .collect(Collectors.toSet());
            
            logger.info("Successfully loaded {} properties, non-sensitive keys: {}", 
                    props.size(), nonSensitiveKeys);
        } catch (IOException e) {
            logger.error("Failed to load .env.properties file", e);
        }
    }
} 