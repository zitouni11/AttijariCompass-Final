package com.adem.attijari_compass.recommendation.expense;

import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpenseInsightServiceTest {

    private final ExpenseMetricsCalculator expenseMetricsCalculator = mock(ExpenseMetricsCalculator.class);
    private final ExpenseInsightService service = new ExpenseInsightService(expenseMetricsCalculator);

    @Test
    void shouldGenerateInsightsFromCategorySpikeAndFixedChargesPressure() {
        Long userId = 1L;
        when(expenseMetricsCalculator.calculate(userId)).thenReturn(snapshotWithSignals());

        List<ExpenseInsight> insights = service.generateInsights(userId);

        assertFalse(insights.isEmpty());
        assertTrue(insights.stream().anyMatch(insight ->
                "CATEGORY_SPIKE".equals(insight.getInsightType())
                        && insight.getCategory() == TransactionCategory.SHOPPING
                        && insight.getEstimatedMonthlyGain() != null
                        && insight.getEstimatedMonthlyGain() > 0.0d));
        assertTrue(insights.stream().anyMatch(insight ->
                "FIXED_CHARGES_PRESSURE".equals(insight.getInsightType())
                        && RecommendationPriority.HIGH == insight.getPriority()));
    }

    @Test
    void shouldReturnEmptyListWhenSnapshotHasNoUsableSignal() {
        when(expenseMetricsCalculator.calculate(2L)).thenReturn(ExpenseMetricsSnapshot.builder().build());

        List<ExpenseInsight> insights = service.generateInsights(2L);

        assertTrue(insights.isEmpty());
    }

    @Test
    void shouldSortHighPriorityInsightsBeforeMediumPriorityOnes() {
        when(expenseMetricsCalculator.calculate(3L)).thenReturn(snapshotWithSignals());

        List<ExpenseInsight> insights = service.generateInsights(3L);

        assertEquals(RecommendationPriority.HIGH, insights.get(0).getPriority());
    }

    @Test
    void shouldUseThirtyDayWordingForDefaultAnalysisWindow() {
        when(expenseMetricsCalculator.calculate(4L)).thenReturn(snapshotWithSignals());

        List<ExpenseInsight> insights = service.generateInsights(4L);

        ExpenseInsight dominanceInsight = insights.stream()
                .filter(insight -> "CATEGORY_DOMINANCE".equals(insight.getInsightType()))
                .findFirst()
                .orElse(null);

        assertNotNull(dominanceInsight);
        assertTrue(dominanceInsight.getExplanation().contains("30 derniers jours"));
        assertFalse(dominanceInsight.getExplanation().contains("du mois"));
    }

    @Test
    void shouldUseNeutralRecentPeriodWordingForExtendedAnalysisWindow() {
        ExpenseMetricsSnapshot snapshot = snapshotWithSignals();
        snapshot.setAnalysisWindowDays(90);

        when(expenseMetricsCalculator.calculate(5L)).thenReturn(snapshot);

        List<ExpenseInsight> insights = service.generateInsights(5L);

        ExpenseInsight dominanceInsight = insights.stream()
                .filter(insight -> "CATEGORY_DOMINANCE".equals(insight.getInsightType()))
                .findFirst()
                .orElse(null);

        assertNotNull(dominanceInsight);
        assertTrue(dominanceInsight.getExplanation().contains("periode recente analysee"));
    }

    @Test
    void shouldScaleDominanceImpactWithCategoryAmount() {
        when(expenseMetricsCalculator.calculate(6L)).thenReturn(snapshotWithSignals());

        List<ExpenseInsight> insights = service.generateInsights(6L);

        ExpenseInsight dominanceInsight = insights.stream()
                .filter(insight -> "CATEGORY_DOMINANCE".equals(insight.getInsightType()))
                .filter(insight -> insight.getCategory() == TransactionCategory.SHOPPING)
                .findFirst()
                .orElse(null);

        assertNotNull(dominanceInsight);
        assertEquals(40.0d, dominanceInsight.getEstimatedMonthlyGain());
    }

    @Test
    void shouldAvoidTooWeakDominanceImpactWhenCategoryAmountIsStillHigh() {
        ExpenseMetricsSnapshot snapshot = ExpenseMetricsSnapshot.builder()
                .analysisWindowDays(30)
                .analysisStartDate(LocalDate.of(2026, 3, 13))
                .analysisEndDate(LocalDate.of(2026, 4, 11))
                .analysisExpenseTotal(new BigDecimal("250.00"))
                .analysisCategoryTotals(new EnumMap<>(Map.of(
                        TransactionCategory.CAFES, new BigDecimal("85.00"),
                        TransactionCategory.ALIMENTATION, new BigDecimal("70.00"),
                        TransactionCategory.TRANSPORT, new BigDecimal("95.00")
                )))
                .analysisCategoryCounts(new EnumMap<>(Map.of(
                        TransactionCategory.CAFES, 2L,
                        TransactionCategory.ALIMENTATION, 2L,
                        TransactionCategory.TRANSPORT, 3L
                )))
                .build();

        when(expenseMetricsCalculator.calculate(7L)).thenReturn(snapshot);

        List<ExpenseInsight> insights = service.generateInsights(7L);

        ExpenseInsight dominanceInsight = insights.stream()
                .filter(insight -> "CATEGORY_DOMINANCE".equals(insight.getInsightType()))
                .filter(insight -> insight.getCategory() == TransactionCategory.CAFES)
                .findFirst()
                .orElse(null);

        assertNotNull(dominanceInsight);
        assertEquals(10.0d, dominanceInsight.getEstimatedMonthlyGain());
    }

    private ExpenseMetricsSnapshot snapshotWithSignals() {
        Map<TransactionCategory, BigDecimal> currentTotals = new EnumMap<>(TransactionCategory.class);
        currentTotals.put(TransactionCategory.SHOPPING, new BigDecimal("400.00"));
        currentTotals.put(TransactionCategory.HOTEL, new BigDecimal("500.00"));
        currentTotals.put(TransactionCategory.STEG_SONEDE, new BigDecimal("350.00"));

        Map<TransactionCategory, BigDecimal> baselineAverages = new EnumMap<>(TransactionCategory.class);
        baselineAverages.put(TransactionCategory.SHOPPING, new BigDecimal("200.00"));
        baselineAverages.put(TransactionCategory.HOTEL, new BigDecimal("430.00"));
        baselineAverages.put(TransactionCategory.STEG_SONEDE, new BigDecimal("200.00"));

        Map<TransactionCategory, Long> currentCounts = new EnumMap<>(TransactionCategory.class);
        currentCounts.put(TransactionCategory.SHOPPING, 4L);
        currentCounts.put(TransactionCategory.HOTEL, 1L);
        currentCounts.put(TransactionCategory.STEG_SONEDE, 2L);

        return ExpenseMetricsSnapshot.builder()
                .analysisWindowDays(30)
                .analysisStartDate(LocalDate.of(2026, 3, 13))
                .analysisEndDate(LocalDate.of(2026, 4, 11))
                .analysisExpenseTotal(new BigDecimal("1250.00"))
                .baselineMonthlyAverage(new BigDecimal("700.00"))
                .analysisCategoryTotals(currentTotals)
                .baselineCategoryAverages(baselineAverages)
                .analysisCategoryCounts(currentCounts)
                .fixedChargesTotal(new BigDecimal("850.00"))
                .analysisIncomeTotal(new BigDecimal("1000.00"))
                .build();
    }
}
