package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.TransactionCategoryFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionCategoryFeedbackRepository extends JpaRepository<TransactionCategoryFeedback, Long> {

    Optional<TransactionCategoryFeedback> findTopByUserIdAndOriginalTextOrderByCreatedAtDesc(Long userId, String originalText);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM TransactionCategoryFeedback feedback
            WHERE feedback.userId = :userId
            """)
    int deleteAllByUserId(@Param("userId") Long userId);
}
