package com.adem.attijari_compass.recommendation.dto;

import com.adem.attijari_compass.recommendation.storytelling.RecommendationStorytellingDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {

    private RecommendationSummaryDto summary;
    private List<RecommendationDto> recommendations;
    private RecommendationStorytellingDto storytelling;
    private Boolean hasActiveGoal;
    private RecommendationGoalContextDto priorityGoal;
    private Integer objectiveImpactMonths;
    private LocalDate currentGoalDate;
    private LocalDate simulatedGoalDate;
}
