package com.adem.attijari_compass.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalRecommendationResponse {
    private String type;
    private String typeLabel;
    private String priority;
    private String priorityLabel;
    private String sourceType;
    private String title;
    private String message;
    private Double estimatedMonthlyImpact;
}
