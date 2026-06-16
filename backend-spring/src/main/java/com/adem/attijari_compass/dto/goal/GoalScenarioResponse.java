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
public class GoalScenarioResponse {
    private String scenarioName;
    private String scenarioLabel;
    private String sourceType;
    private Double suggestedMonthlySaving;
    private Integer monthsToReachGoal;
    private LocalDate predictedAchievementDate;
    private Boolean achievableByTargetDate;
    private Double shortfallAtTargetDate;
    private Double scenarioViabilityScore;
    private Double coverageAtTargetDatePercentage;
    private Double completionPercentageAtTargetDate;
}
