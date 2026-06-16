package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.dto.income.IncomeInsightResponse;
import com.adem.attijari_compass.dto.income.IncomeRecommendation;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import com.adem.attijari_compass.recommendation.enums.RecommendationType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class IncomeRecommendationMapper {

    public RecommendationDto toRecommendationDto(IncomeRecommendation incomeRecommendation,
                                                 IncomeInsightResponse incomeInsight) {
        if (incomeRecommendation == null) {
            return null;
        }

        RecommendationPriority priority = mapPriority(incomeRecommendation.getPriority());
        String normalizedType = normalize(incomeRecommendation.getType());

        return RecommendationDto.builder()
                .title(incomeRecommendation.getTitle())
                .message(incomeRecommendation.getDescription())
                .suggestedAction(buildSuggestedAction(normalizedType))
                .type(mapType(normalizedType))
                .priority(priority)
                .category(incomeRecommendation.getTitle())
                .sourceType(RecommendationSourceType.INCOME.name())
                .estimatedMonthlyGain(null)
                .estimatedGoalImpactMonths(null)
                .confidenceScore(resolveConfidenceScore(incomeInsight, priority))
                .severityScore(resolveSeverityScore(priority))
                .explanation(buildExplanation(incomeRecommendation, incomeInsight))
                .basedOn(buildBasedOn(incomeInsight))
                .actionable(true)
                .build();
    }

    private RecommendationPriority mapPriority(String priority) {
        String normalizedPriority = normalize(priority);
        return switch (normalizedPriority) {
            case "HIGH" -> RecommendationPriority.HIGH;
            case "LOW" -> RecommendationPriority.LOW;
            default -> RecommendationPriority.MEDIUM;
        };
    }

    private RecommendationType mapType(String type) {
        return switch (type) {
            case "SAVING" -> RecommendationType.SAVING_OPPORTUNITY;
            case "RISK" -> RecommendationType.RISK_PREVENTION;
            case "OPTIMIZATION" -> RecommendationType.HABIT_IMPROVEMENT;
            case "STABILITY" -> RecommendationType.HABIT_IMPROVEMENT;
            default -> RecommendationType.HABIT_IMPROVEMENT;
        };
    }

    private String buildSuggestedAction(String type) {
        return switch (type) {
            case "SAVING" -> "Renforcer votre capacite d'epargne";
            case "RISK" -> "Reduire votre exposition au risque de revenu";
            case "OPTIMIZATION" -> "Optimiser l'organisation de vos revenus";
            case "STABILITY" -> "Stabiliser la gestion de vos revenus";
            default -> "Mieux piloter vos revenus";
        };
    }

    private Double resolveConfidenceScore(IncomeInsightResponse incomeInsight, RecommendationPriority priority) {
        if (incomeInsight != null && incomeInsight.getIncomeConfidenceScore() != null) {
            return incomeInsight.getIncomeConfidenceScore().doubleValue();
        }

        return switch (priority) {
            case HIGH -> 84.0d;
            case LOW -> 62.0d;
            default -> 72.0d;
        };
    }

    private Double resolveSeverityScore(RecommendationPriority priority) {
        return switch (priority) {
            case HIGH -> 85.0d;
            case LOW -> 55.0d;
            default -> 70.0d;
        };
    }

    private String buildExplanation(IncomeRecommendation incomeRecommendation, IncomeInsightResponse incomeInsight) {
        if (incomeInsight == null || incomeInsight.getInsightSummary() == null || incomeInsight.getInsightSummary().isBlank()) {
            return incomeRecommendation.getDescription();
        }

        return incomeRecommendation.getDescription() + " " + incomeInsight.getInsightSummary();
    }

    private List<String> buildBasedOn(IncomeInsightResponse incomeInsight) {
        List<String> basedOn = new ArrayList<>();
        basedOn.add("Source: module income");

        if (incomeInsight == null) {
            return List.copyOf(basedOn);
        }

        if (incomeInsight.getPrimaryIncomeType() != null && !incomeInsight.getPrimaryIncomeType().isBlank()) {
            basedOn.add("Type principal: " + incomeInsight.getPrimaryIncomeType());
        }
        if (incomeInsight.getIncomeStability() != null && !incomeInsight.getIncomeStability().isBlank()) {
            basedOn.add("Stabilite: " + incomeInsight.getIncomeStability());
        }
        if (incomeInsight.getIncomeRegularity() != null && !incomeInsight.getIncomeRegularity().isBlank()) {
            basedOn.add("Regularite: " + incomeInsight.getIncomeRegularity());
        }

        return List.copyOf(basedOn);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}
