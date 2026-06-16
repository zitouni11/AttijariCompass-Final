package com.adem.attijari_compass.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationExplanationRequest {

    @NotBlank
    private String title;

    private String category;
    private String type;
    private Double amount;
    private String period;
    private Double targetedTransactionsTotal;
    private Double monthlyImpactEstimated;
    private Integer potentialPercent;
    private String goalTitle;
    private Integer financialScore;
    private String globalStatus;
    private Double savingsRate;
    private String message;
    private String suggestedAction;
}
