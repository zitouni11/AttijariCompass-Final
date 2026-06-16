package com.adem.attijari_compass.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalPredictionResponse {
    private Long goalId;
    private Double remainingAmount;
    private Integer remainingMonths;
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
}
