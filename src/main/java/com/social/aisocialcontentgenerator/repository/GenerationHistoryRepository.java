package com.social.aisocialcontentgenerator.repository;


import com.social.aisocialcontentgenerator.entity.GenerationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GenerationHistoryRepository extends JpaRepository<GenerationHistory, Long> {
    List<GenerationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}

