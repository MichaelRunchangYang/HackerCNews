package com.example.hacker_cnews.entity;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "translated_news")
public class NewsItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    private Long id;  // 使用Hacker News的ID作为主键，不自动生成
    
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
    
    // 新增字段 - 原始分数
    private Integer score;
    
    // 新增字段 - 最近一次在Hacker News上的排名
    @Column(name = "`rank`")
    private Integer rank;
    
    // 新增字段 - 最后更新时间
    private Instant lastUpdated;

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
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
    
    public Integer getRank() {
        return rank;
    }
    
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    
    public Instant getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
} 