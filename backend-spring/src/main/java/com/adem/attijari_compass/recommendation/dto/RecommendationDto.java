package com.adem.attijari_compass.recommendation.dto;

import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import com.adem.attijari_compass.recommendation.enums.RecommendationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {

    private String title;
    private String message;
    private String suggestedAction;
    private RecommendationType type;
    private RecommendationPriority priority;
    private String category;
    private String sourceType;
    private Double estimatedMonthlyGain;
    private Double targetedTransactionsTotal;
    private Double estimatedGoalImpactMonths;
    private Double confidenceScore;
    private Double severityScore;
    private String explanation;
    private List<String> basedOn;
    private Boolean actionable;

}
