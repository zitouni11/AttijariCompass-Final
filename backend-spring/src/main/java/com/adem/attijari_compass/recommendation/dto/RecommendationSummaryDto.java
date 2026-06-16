package com.adem.attijari_compass.recommendation.dto;

import com.adem.attijari_compass.recommendation.enums.CurrentMonthSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationSummaryDto {

    private Integer totalRecommendations;
    private Integer criticalCount;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;
    private Double totalEstimatedMonthlyGain;
    private Integer financialScore;
    private String financialScoreLabel;
    private FinancialScoreBreakdownDto financialScoreBreakdown;
    private CurrentMonthSeverity currentMonthSeverity;
    private String currentMonthStatusLabel;
    private Double currentMonthIncome;
    private Double currentMonthExpenses;
    private Double currentMonthNetBalance;
    private Double currentMonthSavingsRate;
    private String globalStatus;
    private String aiSummary;
}
