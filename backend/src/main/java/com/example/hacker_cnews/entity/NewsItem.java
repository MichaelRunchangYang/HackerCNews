package com.example.hacker_cnews.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "translated_news")
public class NewsItem {
    @Id
    private Long id;
    
    @Column(length = 500)
    private String titleEn;
    
    @Column(length = 500)
    private String titleZh;
    
    @Column(length = 1000)
    private String url;
    
    @Column(columnDefinition = "TEXT")
    private String textEn;
    
    @Column(columnDefinition = "TEXT")
    private String textZh;
    
    @Column(columnDefinition = "JSON")
    private String commentIds;
    
    private Long time;
    
    @Column(length = 50)
    private String type;
    
    private Instant createdAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getTitleZh() {
        return titleZh;
    }

    public void setTitleZh(String titleZh) {
        this.titleZh = titleZh;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTextEn() {
        return textEn;
    }

    public void setTextEn(String textEn) {
        this.textEn = textEn;
    }

    public String getTextZh() {
        return textZh;
    }

    public void setTextZh(String textZh) {
        this.textZh = textZh;
    }

    public String getCommentIds() {
        return commentIds;
    }

    public void setCommentIds(String commentIds) {
        this.commentIds = commentIds;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
} 