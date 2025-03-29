package com.example.hacker_cnews.repository;

import com.example.hacker_cnews.entity.NewsItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {
    List<NewsItem> findTop30ByOrderByTimeDesc();
} 