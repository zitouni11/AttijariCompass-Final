package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.expense.ExpenseInsight;
import com.adem.attijari_compass.recommendation.expense.ExpenseInsightService;
import com.adem.attijari_compass.recommendation.expense.ExpenseMetricsCalculator;
import com.adem.attijari_compass.recommendation.expense.ExpenseMetricsSnapshot;
import com.adem.attijari_compass.recommendation.expense.ExpenseRecommendationMapper;
import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.FinancialScoreBreakdownDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationResponseDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationSummaryDto;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import com.adem.attijari_compass.recommendation.enums.RecommendationType;
import com.adem.attijari_compass.recommendation.storytelling.RecommendationStorytellingDto;
import com.adem.attijari_compass.recommendation.storytelling.RecommendationStorytellingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceImplTest {

    @Test
    void shouldMergeExpenseRecommendationsAndLimitIncomeRecommendations() {
        ExpenseInsightService expenseInsightService = mock(ExpenseInsightService.class);
        ExpenseMetricsCalculator expenseMetricsCalculator = mock(ExpenseMetricsCalculator.class);
        FinancialScoreService financialScoreService = mock(FinancialScoreService.class);
        when(expenseMetricsCalculator.calculate(1L)).thenReturn(ExpenseMetricsSnapshot.builder().build());
        when(financialScoreService.calculate(eq(1L), any(FinancialInsightDto.class), any(ExpenseMetricsSnapshot.class)))
                .thenReturn(FinancialScoreBreakdownDto.builder()
                        .finalScore(58)
                        .label("A consolider")
                        .build());
        when(expenseInsightService.generateInsights(any(ExpenseMetricsSnapshot.class))).thenReturn(List.of(
                ExpenseInsight.builder()
                        .insightType("FIXED_CHARGES_PRESSURE")
                        .title("Reduire la pression des charges fixes")
                        .message("Vos depenses de logement et factures limitent fortement votre marge mensuelle.")
                        .suggestedAction("Identifier les charges incompressibles et les postes renegociables.")
                        .priority(RecommendationPriority.HIGH)
                        .severityScore(92.0d)
                        .confidenceScore(88.0d)
                        .estimatedMonthlyGain(180.0d)
                        .build(),
                ExpenseInsight.builder()
                        .insightType("CATEGORY_SPIKE")
                        .title("Maitriser les depenses shopping")
                        .message("Les depenses de shopping depassent nettement votre rythme recent.")
                        .suggestedAction("Fixer un plafond shopping.")
                        .priority(RecommendationPriority.MEDIUM)
                        .severityScore(81.0d)
                        .confidenceScore(80.0d)
                        .estimatedMonthlyGain(120.0d)
                        .build()
        ));
        RecommendationStorytellingService recommendationStorytellingService = mock(RecommendationStorytellingService.class);
        when(recommendationStorytellingService.generateStory(org.mockito.ArgumentMatchers.any(RecommendationResponseDto.class)))
                .thenReturn(RecommendationStorytellingDto.builder()
                        .summary("Synthese")
                        .mainConcern("Pression des charges fixes")
                        .opportunity("Leviers d'optimisation")
                        .action("Commencer par les charges fixes")
                        .build());

        RecommendationServiceImpl service = new RecommendationServiceImpl(
                new StubFinancialAnalysisService(),
                new StubRecommendationEngineService(),
                new StubRecommendationNarrativeService(),
                new StubIncomeRecommendationSourceService(),
                expenseInsightService,
                expenseMetricsCalculator,
                financialScoreService,
                new ExpenseRecommendationMapper(),
                recommendationStorytellingService,
                mock(com.adem.attijari_compass.repository.UserRepository.class),
                mock(com.adem.attijari_compass.repository.FinancialGoalRepository.class)
        );

        RecommendationResponseDto response = service.generateRecommendationsForUser(1L);

        assertEquals(5, response.getRecommendations().size());
        assertEquals("Reduire la pression des charges fixes", response.getRecommendations().get(0).getTitle());
        assertEquals(RecommendationSourceType.EXPENSE.name(), response.getRecommendations().get(0).getSourceType());
        assertEquals(5, response.getSummary().getTotalRecommendations());
        assertEquals(3, response.getSummary().getHighCount());
        assertEquals(600.0d, response.getSummary().getTotalEstimatedMonthlyGain());
        assertEquals(58, response.getSummary().getFinancialScore());
        assertEquals("A consolider", response.getSummary().getFinancialScoreLabel());
        assertTrue(response.getRecommendations().stream().anyMatch(recommendation ->
                RecommendationSourceType.EXPENSE.name().equals(recommendation.getSourceType())
                        && recommendation.getTitle().equals("Maitriser les depenses shopping")));
        assertTrue(response.getRecommendations().stream().anyMatch(recommendation ->
                recommendation.getTitle().equals("Renforcer votre epargne")));
        assertEquals(2, response.getRecommendations().stream()
                .map(RecommendationDto::getSourceType)
                .filter(RecommendationSourceType.INCOME.name()::equals)
                .count());
        assertNotNull(response.getStorytelling());
        assertEquals("Synthese", response.getStorytelling().getSummary());
    }

    private static final class StubFinancialAnalysisService implements FinancialAnalysisService {

        @Override
        public FinancialInsightDto analyzeUserFinancials(Long userId) {
            return FinancialInsightDto.builder()
                    .totalIncome(5000.0d)
                    .totalExpenses(4200.0d)
                    .savingsRate(8.0d)
                    .savingsTooLow(true)
                    .build();
        }

        @Override
        public FinancialInsightDto analyzeUserFinancials(String email) {
            return analyzeUserFinancials(1L);
        }
    }

    private static final class StubRecommendationEngineService implements RecommendationEngineService {

        @Override
        public List<RecommendationDto> generateRecommendations(FinancialInsightDto insight) {
            return List.of(RecommendationDto.builder()
                    .title("Renforcer votre epargne")
                    .message("Votre taux d'epargne reste faible.")
                    .suggestedAction("Automatisez votre epargne.")
                    .type(RecommendationType.SAVING_OPPORTUNITY)
                    .priority(RecommendationPriority.HIGH)
                    .category("EPARGNE")
                    .sourceType(RecommendationSourceType.EXPENSE.name())
                    .estimatedMonthlyGain(300.0d)
                    .confidenceScore(88.0d)
                    .severityScore(70.0d)
                    .actionable(true)
                    .build());
        }
    }

    private static final class StubRecommendationNarrativeService implements RecommendationNarrativeService {

        @Override
        public RecommendationSummaryDto buildSummary(
                FinancialInsightDto insight,
                FinancialScoreBreakdownDto scoreBreakdown,
                List<RecommendationDto> recommendations
        ) {
            double totalEstimatedMonthlyGain = recommendations.stream()
                    .map(RecommendationDto::getEstimatedMonthlyGain)
                    .filter(java.util.Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            return RecommendationSummaryDto.builder()
                    .totalRecommendations(recommendations.size())
                    .highCount((int) recommendations.stream()
                            .map(RecommendationDto::getPriority)
                            .filter(RecommendationPriority.HIGH::equals)
                            .count())
                    .mediumCount((int) recommendations.stream()
                            .map(RecommendationDto::getPriority)
                            .filter(RecommendationPriority.MEDIUM::equals)
                            .count())
                    .lowCount((int) recommendations.stream()
                            .map(RecommendationDto::getPriority)
                            .filter(RecommendationPriority.LOW::equals)
                            .count())
                    .criticalCount(0)
                    .totalEstimatedMonthlyGain(totalEstimatedMonthlyGain)
                    .globalStatus("OK")
                    .aiSummary("summary")
                    .build();
        }
    }

    private static final class StubIncomeRecommendationSourceService implements IncomeRecommendationSourceService {

        @Override
        public List<RecommendationDto> generateRecommendationsForUser(Long userId) {
            return List.of(
                    RecommendationDto.builder()
                            .title("Optimiser vos revenus multiples")
                            .message("Vous semblez disposer de plusieurs sources de revenus.")
                            .suggestedAction("Optimiser l'organisation de vos revenus")
                            .type(RecommendationType.HABIT_IMPROVEMENT)
                            .priority(RecommendationPriority.MEDIUM)
                            .category("Optimiser vos revenus multiples")
                            .sourceType(RecommendationSourceType.INCOME.name())
                            .estimatedMonthlyGain(null)
                            .confidenceScore(78.0d)
                            .severityScore(85.0d)
                            .actionable(true)
                            .build(),
                    RecommendationDto.builder()
                            .title("Constituer un buffer de revenu")
                            .message("Vos revenus variables meritent un buffer financier.")
                            .suggestedAction("Prevoir une reserve de tresorerie")
                            .type(RecommendationType.RISK_PREVENTION)
                            .priority(RecommendationPriority.HIGH)
                            .category("BUFFER")
                            .sourceType(RecommendationSourceType.INCOME.name())
                            .estimatedMonthlyGain(null)
                            .confidenceScore(74.0d)
                            .severityScore(72.0d)
                            .actionable(true)
                            .build(),
                    RecommendationDto.builder()
                            .title("Diversifier vos revenus")
                            .message("Une troisieme recommandation income doit etre filtree.")
                            .suggestedAction("Explorer une source complementaire")
                            .type(RecommendationType.HABIT_IMPROVEMENT)
                            .priority(RecommendationPriority.LOW)
                            .category("DIVERSIFICATION")
                            .sourceType(RecommendationSourceType.INCOME.name())
                            .estimatedMonthlyGain(null)
                            .confidenceScore(61.0d)
                            .severityScore(40.0d)
                            .actionable(true)
                            .build()
            );
        }

        @Override
        public List<RecommendationDto> generateRecommendationsForUser(String email) {
            return generateRecommendationsForUser(1L);
        }
    }
}
