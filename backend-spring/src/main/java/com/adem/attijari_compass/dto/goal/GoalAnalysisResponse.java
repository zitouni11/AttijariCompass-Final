package com.adem.attijari_compass.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalAnalysisResponse {
    private Long goalId;
    private Integer monthsAnalyzed;
    private Integer transactionCount;
    private Boolean enoughData;
    private String analysisMessage;
    private Double averageMonthlyIncome;
    private Double averageMonthlyExpenses;
    private Double fixedExpenses;
    private Double estimatedFixedExpenses;
    private Double estimatedEssentialExpenses;
    private Double compressibleExpenses;
    private Double estimatedCompressibleExpenses;
    private Double prudentSavingCapacity;
    private Double balancedSavingCapacity;
    private Double aggressiveSavingCapacity;
    private Double averageHistoricalSavings;
    private Double incomeStabilityScore;
    private Double expenseStabilityScore;
    private List<GoalBlockingCategoryResponse> blockingCategories;
}
