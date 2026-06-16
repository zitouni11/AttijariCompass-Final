package com.adem.attijari_compass.dto.goal;

import com.adem.attijari_compass.entity.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalAiRefreshResponse {
    private Long id;
    private String name;
    private String description;
    private Double targetAmount;
    private Double currentAmount;
    private LocalDate targetDate;
    private GoalStatus status;
    private LocalDateTime createdAt;
    private Double progressPercentage;
    private Double remainingAmount;
    private Double monthlySavingsRequired;

    private Double requiredMonthlySaving;
    private Double feasibilityScore;
    private Double successProbability;
    private String riskLevel;
    private LocalDate predictedAchievementDate;
    private String predictedAchievementScenario;
    private String recommendedScenario;
    private Boolean achievableByTargetDate;
    private Double shortfallAtTargetDate;
    private Double balancedCapacity;

    private GoalAnalysisResponse analysis;
    private GoalPredictionResponse prediction;
    private List<GoalBlockingCategoryResponse> blockingCategories;
    private List<GoalScenarioResponse> simulations;
    private List<GoalRecommendationResponse> recommendations;
    private GoalStorytellingResponse storytelling;
}
