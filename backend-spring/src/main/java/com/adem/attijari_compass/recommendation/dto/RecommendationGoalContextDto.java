package com.adem.attijari_compass.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationGoalContextDto {
    private Long id;
    private String title;
    private Double targetAmount;
    private Double currentAmount;
    private LocalDate targetDate;
}
