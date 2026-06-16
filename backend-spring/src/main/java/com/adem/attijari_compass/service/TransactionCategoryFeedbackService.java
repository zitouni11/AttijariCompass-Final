package com.adem.attijari_compass.service;

import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionCategoryFeedback;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.model.categorization.CategorizationSources;
import com.adem.attijari_compass.repository.TransactionCategoryFeedbackRepository;
import com.adem.attijari_compass.util.TransactionTextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionCategoryFeedbackService {

    private final TransactionCategoryFeedbackRepository feedbackRepository;

    @Transactional(readOnly = true)
    public Optional<CategorizationResult> findCorrection(String merchantName, String description, Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String normalizedText = TransactionTextNormalizer.normalize(merchantName, description);
        if (normalizedText.isBlank()) {
            return Optional.empty();
        }

        return feedbackRepository.findTopByUserIdAndOriginalTextOrderByCreatedAtDesc(userId, normalizedText)
                .map(feedback -> CategorizationResult.builder()
                        .category(feedback.getCorrectedCategory())
                        .confidence(1.0d)
                        .source(CategorizationSources.USER_FEEDBACK)
                        .reason("user_feedback:" + feedback.getCorrectedCategory().name().toLowerCase())
                        .normalizedText(normalizedText)
                        .build());
    }

    public void recordCorrection(
            Long transactionId,
            TransactionCategory predictedCategory,
            TransactionCategory correctedCategory,
            String originalText,
            Double confidence,
            String source,
            Long userId,
            String merchantName,
            String description
    ) {
        if (predictedCategory == null || correctedCategory == null) {
            return;
        }

        if (predictedCategory == correctedCategory) {
            return;
        }

        if (source == null || source.isBlank()) {
            log.debug("Skipping feedback because no prediction source is available");
            return;
        }

        String normalizedText = originalText;
        if (normalizedText == null || normalizedText.isBlank()) {
            normalizedText = TransactionTextNormalizer.normalize(merchantName, description);
        }

        TransactionCategoryFeedback feedback = TransactionCategoryFeedback.builder()
                .transactionId(transactionId)
                .originalText(normalizedText)
                .predictedCategory(predictedCategory)
                .correctedCategory(correctedCategory)
                .confidence(confidence != null ? confidence : 0.0d)
                .createdAt(LocalDateTime.now())
                .source(source)
                .userId(userId)
                .build();

        feedbackRepository.save(feedback);
    }

    public void recordCorrection(Transaction transaction, TransactionCategory correctedCategory, Long userId) {
        if (transaction == null || correctedCategory == null) {
            return;
        }

        recordCorrection(
                transaction.getId(),
                transaction.getCategory(),
                correctedCategory,
                transaction.getCategorizationNormalizedText(),
                transaction.getCategorizationConfidence(),
                transaction.getCategorizationSource(),
                userId,
                transaction.getMerchantName(),
                transaction.getDescription()
        );
    }
}
