package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationEngineServiceImplTest {

    private final RecommendationEngineServiceImpl service =
            new RecommendationEngineServiceImpl(new RecommendationScoringServiceImpl());

    @Test
    void shouldMarkSpendingRecommendationsAsExpenseSource() {
        FinancialInsightDto insight = FinancialInsightDto.builder()
                .totalIncome(4000.0d)
                .totalExpenses(3900.0d)
                .savingsRate(5.0d)
                .savingsAmount(100.0d)
                .possibleSavingsPotential(200.0d)
                .savingsTooLow(true)
                .build();

        List<RecommendationDto> recommendations = service.generateRecommendations(insight);

        assertEquals(1, recommendations.size());
        assertEquals(RecommendationSourceType.EXPENSE.name(), recommendations.get(0).getSourceType());
    }

    @Test
    void shouldMarkGoalRecommendationsAsGoalSource() {
        FinancialInsightDto insight = FinancialInsightDto.builder()
                .currentMonthlyContribution(200.0d)
                .requiredMonthlyContributionForGoal(450.0d)
                .goalDelayed(true)
                .build();

        List<RecommendationDto> recommendations = service.generateRecommendations(insight);

        assertEquals(1, recommendations.size());
        assertEquals(RecommendationSourceType.GOAL.name(), recommendations.get(0).getSourceType());
    }
}
