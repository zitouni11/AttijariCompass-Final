package com.adem.attijari_compass.service;

import com.adem.attijari_compass.config.CategorizationMlProperties;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.model.categorization.CategorizationSources;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SmartCategorizationService {

    private static final double RULE_CONFIDENCE_THRESHOLD = 0.90d;
    private static final double RULE_FALLBACK_THRESHOLD = 0.60d;

    private final CategoryEngineService categoryEngineService;
    private final MlCategorizationService mlCategorizationService;
    private final CategorizationMlProperties properties;
    private final TransactionCategoryFeedbackService transactionCategoryFeedbackService;

    public CategorizationResult categorize(String merchantName, String description) {
        return categorize(merchantName, description, null);
    }

    public CategorizationResult categorize(String merchantName, String description, Long userId) {
        Optional<CategorizationResult> feedbackResult = transactionCategoryFeedbackService.findCorrection(
                merchantName,
                description,
                userId
        );
        if (feedbackResult.isPresent()) {
            return feedbackResult.get();
        }

        CategorizationResult ruleResult = categoryEngineService.categorize(merchantName, description);
        if (isReliableRule(ruleResult)) {
            return ruleResult;
        }

        if (!properties.isEnabled()) {
            return fallbackFromRule(ruleResult);
        }

        Optional<CategorizationResult> mlResultOptional = mlCategorizationService.categorize(merchantName, description);
        if (mlResultOptional.isEmpty()) {
            return fallbackFromRule(ruleResult);
        }

        CategorizationResult mlResult = mlResultOptional.get();
        if (mlResult.getConfidence() >= properties.getThreshold()) {
            return mlResult;
        }

        return CategorizationResult.builder()
                .category(TransactionCategory.fallback())
                .confidence(mlResult.getConfidence())
                .source(CategorizationSources.ML_LOW_CONFIDENCE)
                .reason("ml_low_confidence")
                .normalizedText(mlResult.getNormalizedText())
                .build();
    }

    private boolean isReliableRule(CategorizationResult ruleResult) {
        return ruleResult.getCategory() != TransactionCategory.fallback()
                && ruleResult.getConfidence() >= RULE_CONFIDENCE_THRESHOLD;
    }

    private CategorizationResult fallbackFromRule(CategorizationResult ruleResult) {
        TransactionCategory fallbackCategory = ruleResult.getCategory();
        double fallbackConfidence = ruleResult.getConfidence();

        if (fallbackCategory == TransactionCategory.fallback() || fallbackConfidence < RULE_FALLBACK_THRESHOLD) {
            fallbackCategory = TransactionCategory.fallback();
            fallbackConfidence = 0.0d;
        }

        return CategorizationResult.builder()
                .category(fallbackCategory)
                .confidence(fallbackConfidence)
                .source(CategorizationSources.FALLBACK)
                .reason(ruleResult.getReason() != null && !ruleResult.getReason().isBlank()
                        ? "fallback:" + ruleResult.getReason()
                        : "fallback:rule_engine")
                .normalizedText(ruleResult.getNormalizedText())
                .build();
    }
}
