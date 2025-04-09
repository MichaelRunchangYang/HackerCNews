package com.example.hacker_cnews.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.example.hacker_cnews.entity.NewsItem;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用StringRedisSerializer来序列化和反序列化redis的key和value
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public RedisTemplate<String, NewsItem> newsItemRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, NewsItem> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置key的序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        
        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        ObjectMapper mapper = new ObjectMapper();
        // 注册JavaTimeModule以正确处理Java 8日期时间类型
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 激活默认typing，确保反序列化时能正确识别对象类型
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        Jackson2JsonRedisSerializer<NewsItem> serializer = new Jackson2JsonRedisSerializer<>(mapper, NewsItem.class);
        
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
} 