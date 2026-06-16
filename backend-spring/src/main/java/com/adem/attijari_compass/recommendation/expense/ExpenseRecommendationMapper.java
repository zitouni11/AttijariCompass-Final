package com.adem.attijari_compass.recommendation.expense;

import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import com.adem.attijari_compass.recommendation.enums.RecommendationType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class ExpenseRecommendationMapper {

    public RecommendationDto toRecommendation(ExpenseInsight insight) {
        if (insight == null) {
            return null;
        }

        return RecommendationDto.builder()
                .title(insight.getTitle())
                .message(insight.getMessage())
                .suggestedAction(insight.getSuggestedAction())
                .type(mapType(insight.getInsightType()))
                .priority(insight.getPriority())
                .category(resolveCategory(insight))
                .sourceType(RecommendationSourceType.EXPENSE.name())
                .estimatedMonthlyGain(insight.getEstimatedMonthlyGain())
                .targetedTransactionsTotal(insight.getTargetedTransactionsTotal())
                .estimatedGoalImpactMonths(null)
                .confidenceScore(insight.getConfidenceScore())
                .severityScore(insight.getSeverityScore())
                .explanation(insight.getExplanation())
                .basedOn(copyBasedOn(insight.getBasedOn()))
                .actionable(true)
                .build();
    }

    public List<RecommendationDto> toRecommendations(List<ExpenseInsight> insights) {
        if (insights == null || insights.isEmpty()) {
            return List.of();
        }

        return insights.stream()
                .map(this::toRecommendation)
                .filter(Objects::nonNull)
                .toList();
    }

    private RecommendationType mapType(String insightType) {
        String normalizedInsightType = normalize(insightType);
        return switch (normalizedInsightType) {
            case "MONTHLY_TOTAL_SPIKE" -> RecommendationType.BUDGET_ALERT;
            case "FIXED_CHARGES_PRESSURE" -> RecommendationType.RISK_PREVENTION;
            case "CATEGORY_SPIKE", "CATEGORY_DOMINANCE" -> RecommendationType.HABIT_IMPROVEMENT;
            default -> RecommendationType.HABIT_IMPROVEMENT;
        };
    }

    private String resolveCategory(ExpenseInsight insight) {
        if (insight.getCategory() != null) {
            return insight.getCategory().name();
        }

        return switch (normalize(insight.getInsightType())) {
            case "FIXED_CHARGES_PRESSURE" -> "CHARGES_FIXES";
            default -> "GENERAL";
        };
    }

    private List<String> copyBasedOn(List<String> basedOn) {
        if (basedOn == null || basedOn.isEmpty()) {
            return List.of();
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
