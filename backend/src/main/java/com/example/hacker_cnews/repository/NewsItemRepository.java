package com.example.hacker_cnews.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hacker_cnews.entity.NewsItem;

@Repository
public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {
    List<NewsItem> findTop30ByOrderByTimeDesc();
    List<NewsItem> findTop50ByOrderByTimeDesc();
} 