package com.example.hacker_cnews.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated 已被 {@link PropertiesConfig} 替代，该类使用更可靠的方式加载环境变量
 * 保留此类是为了兼容性，将在未来版本中移除
 */
@Deprecated
@Configuration
@PropertySource("classpath:.env.properties")
public class DotenvConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DotenvConfig.class);
    
    @Autowired
    private Environment env;
    
    @PostConstruct
    public void logEnvVars() {
        // 仅用于调试 - 确认环境变量已正确加载
        logger.info("DotenvConfig is deprecated and will be removed in future versions");
        logger.info("Please use PropertiesConfig instead");
    }
} 