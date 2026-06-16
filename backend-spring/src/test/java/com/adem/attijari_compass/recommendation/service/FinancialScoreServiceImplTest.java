package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.FinancialScoreBreakdownDto;
import com.adem.attijari_compass.recommendation.enums.CurrentMonthSeverity;
import com.adem.attijari_compass.recommendation.expense.ExpenseMetricsSnapshot;
import com.adem.attijari_compass.repository.BudgetTargetRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.service.BudgetTargetMonitoringService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinancialScoreServiceImplTest {

    private static final LocalDate ANALYSIS_END_DATE = LocalDate.of(2026, 4, 11);

    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final BudgetTargetRepository budgetTargetRepository = mock(BudgetTargetRepository.class);
    private final BudgetTargetMonitoringService budgetTargetMonitoringService = mock(BudgetTargetMonitoringService.class);
    private final FinancialScoreServiceImpl service = new FinancialScoreServiceImpl(
            transactionRepository,
            budgetTargetRepository,
            budgetTargetMonitoringService
    );

    @Test
    void shouldReturnExcellentScoreForHealthyScenario() {
        when(transactionRepository.findAllByUserIdAndDateBetween(eq(1L), any(LocalDate.class), eq(ANALYSIS_END_DATE)))
                .thenReturn(incomeTransactions(
                        1000.0d,
                        1000.0d,
                        1000.0d,
                        1000.0d
                ));

        FinancialScoreBreakdownDto score = service.calculate(
                1L,
                FinancialInsightDto.builder()
                        .savingsRate(22.0d)
                        .anomalyDetected(false)
                        .currentMonthlyContribution(450.0d)
                        .requiredMonthlyContributionForGoal(400.0d)
                        .build(),
                snapshot(
                        30,
                        1500.0d,
                        1450.0d,
                        550.0d,
                        4000.0d,
                        Map.of(
                                TransactionCategory.HOTEL, 400.0d,
                                TransactionCategory.ALIMENTATION, 350.0d,
                                TransactionCategory.DIVERTISSEMENT, 250.0d,
                                TransactionCategory.CAFES, 200.0d,
                                TransactionCategory.TRANSPORT, 150.0d,
                                TransactionCategory.STEG_SONEDE, 150.0d
                        )
                )
        );

        assertEquals(100, score.getFinalScore());
        assertEquals("Excellent", score.getLabel());
        assertEquals(CurrentMonthSeverity.NORMAL, score.getCurrentMonthSeverity());
        assertEquals(0, score.getPenaltyPoints());
        assertEquals(16, score.getRawBonusPoints());
        assertEquals(12, score.getBonusPoints());
        assertTrue(Boolean.TRUE.equals(score.getBonusCapApplied()));
        assertEquals(3, score.getBonuses().size());
    }

    @Test
    void shouldPenalizeExpenseDriftAndBudgetConcentration() {
        when(transactionRepository.findAllByUserIdAndDateBetween(eq(2L), any(LocalDate.class), eq(ANALYSIS_END_DATE)))
                .thenReturn(incomeTransactions(
                        1600.0d,
                        0.0d,
                        1400.0d,
                        0.0d
                ));

        FinancialScoreBreakdownDto score = service.calculate(
                2L,
                FinancialInsightDto.builder()
                        .savingsRate(6.0d)
                        .anomalyDetected(false)
                        .build(),
                snapshot(
                        30,
                        2400.0d,
                        1300.0d,
                        550.0d,
                        1300.0d,
                        Map.of(
                                TransactionCategory.ALIMENTATION, 1050.0d,
                                TransactionCategory.CAFES, 800.0d,
                                TransactionCategory.HOTEL, 350.0d,
                                TransactionCategory.STEG_SONEDE, 200.0d
                        )
                )
        );

        assertEquals(49, score.getFinalScore());
        assertEquals("A surveiller", score.getLabel());
        assertEquals(CurrentMonthSeverity.NORMAL, score.getCurrentMonthSeverity());
        assertEquals(-51, score.getPenaltyPoints());
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "RECENT_EXPENSE_SPIKE".equals(factor.getCode()) && Integer.valueOf(-30).equals(factor.getPoints())));
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "DOMINANT_CATEGORY".equals(factor.getCode()) && Integer.valueOf(-10).equals(factor.getPoints())));
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "SECOND_DOMINANT_CATEGORY".equals(factor.getCode()) && Integer.valueOf(-5).equals(factor.getPoints())));
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "FIXED_CHARGES_PRESSURE".equals(factor.getCode()) && Integer.valueOf(-6).equals(factor.getPoints())));
    }

    @Test
    void shouldApplyStrongPenaltyWhenAnomalyIsDetected() {
        when(transactionRepository.findAllByUserIdAndDateBetween(eq(3L), any(LocalDate.class), eq(ANALYSIS_END_DATE)))
                .thenReturn(incomeTransactions(
                        1100.0d,
                        800.0d,
                        1000.0d,
                        900.0d
                ));

        FinancialScoreBreakdownDto score = service.calculate(
                3L,
                FinancialInsightDto.builder()
                        .savingsRate(10.0d)
                        .anomalyDetected(true)
                        .anomalyAmount(650.0d)
                        .build(),
                snapshot(
                        30,
                        1600.0d,
                        1500.0d,
                        600.0d,
                        2600.0d,
                        Map.of(
                                TransactionCategory.HOTEL, 400.0d,
                                TransactionCategory.SHOPPING, 450.0d,
                                TransactionCategory.ALIMENTATION, 350.0d,
                                TransactionCategory.STEG_SONEDE, 200.0d,
                                TransactionCategory.CAFES, 200.0d
                        )
                )
        );

        assertEquals(87, score.getFinalScore());
        assertEquals("Excellent", score.getLabel());
        assertEquals(CurrentMonthSeverity.NORMAL, score.getCurrentMonthSeverity());
        assertEquals(-15, score.getPenaltyPoints());
        assertEquals(2, score.getBonusPoints());
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "EXPENSE_ANOMALY".equals(factor.getCode()) && Integer.valueOf(-15).equals(factor.getPoints())));
        assertTrue(score.getBonuses().stream().anyMatch(factor ->
                "INCOME_STABILITY".equals(factor.getCode()) && Integer.valueOf(2).equals(factor.getPoints())));
    }

    @Test
    void shouldApplySeverePenaltyAndCapBonusesWhenExpenseSpikeExceedsThreeHundredPercent() {
        when(transactionRepository.findAllByUserIdAndDateBetween(eq(4L), any(LocalDate.class), eq(ANALYSIS_END_DATE)))
                .thenReturn(incomeTransactions(
                        1000.0d,
                        1000.0d,
                        1000.0d,
                        1000.0d
                ));

        FinancialScoreBreakdownDto score = service.calculate(
                4L,
                FinancialInsightDto.builder()
                        .savingsRate(22.0d)
                        .anomalyDetected(false)
                        .currentMonthlyContribution(420.0d)
                        .requiredMonthlyContributionForGoal(400.0d)
                        .build(),
                snapshot(
                        30,
                        2100.0d,
                        500.0d,
                        300.0d,
                        4000.0d,
                        Map.of(
                                TransactionCategory.ALIMENTATION, 600.0d,
                                TransactionCategory.CAFES, 550.0d,
                                TransactionCategory.SHOPPING, 500.0d,
                                TransactionCategory.TRANSPORT, 450.0d
                        )
                )
        );

        assertEquals(35, score.getFinalScore());
        assertEquals("A surveiller", score.getLabel());
        assertEquals(CurrentMonthSeverity.NORMAL, score.getCurrentMonthSeverity());
        assertEquals(-70, score.getPenaltyPoints());
        assertEquals(16, score.getRawBonusPoints());
        assertEquals(5, score.getBonusPoints());
        assertTrue(Boolean.TRUE.equals(score.getBonusCapApplied()));
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "RECENT_EXPENSE_SPIKE".equals(factor.getCode()) && Integer.valueOf(-30).equals(factor.getPoints())));
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "SEVERE_EXPENSE_SPIKE".equals(factor.getCode()) && Integer.valueOf(-40).equals(factor.getPoints())));
    }

    @Test
    void shouldCapScoreAndMarkMonthCriticalWhenIncomeIsZeroWithExpenses() {
        when(transactionRepository.findAllByUserIdAndDateBetween(eq(5L), any(LocalDate.class), eq(ANALYSIS_END_DATE)))
                .thenReturn(List.of());

        FinancialScoreBreakdownDto score = service.calculate(
                5L,
                FinancialInsightDto.builder()
                        .totalIncome(0.0d)
                        .totalExpenses(950.0d)
                        .remainingBalance(-950.0d)
                        .savingsRate(0.0d)
                        .anomalyDetected(false)
                        .build(),
                snapshot(
                        30,
                        950.0d,
                        400.0d,
                        250.0d,
                        0.0d,
                        Map.of(
                                TransactionCategory.ALIMENTATION, 350.0d,
                                TransactionCategory.LOGEMENT, 300.0d,
                                TransactionCategory.CAFES, 150.0d,
                                TransactionCategory.TRANSPORT, 150.0d
                        )
                )
        );

        assertEquals(0, score.getFinalScore());
        assertEquals("Critique", score.getLabel());
        assertEquals(CurrentMonthSeverity.CRITICAL, score.getCurrentMonthSeverity());
        assertTrue(Boolean.TRUE.equals(score.getCriticalMonthlySituation()));
        assertEquals(Integer.valueOf(35), score.getAppliedScoreCap());
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "NO_INCOME_WITH_EXPENSES".equals(factor.getCode()) && Integer.valueOf(-40).equals(factor.getPoints())));
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "NEGATIVE_NET_BALANCE".equals(factor.getCode()) && Integer.valueOf(-25).equals(factor.getPoints())));
        assertTrue(score.getPenalties().stream().anyMatch(factor ->
                "ZERO_SAVINGS_WITH_SIGNIFICANT_EXPENSES".equals(factor.getCode()) && Integer.valueOf(-15).equals(factor.getPoints())));
    }

    private ExpenseMetricsSnapshot snapshot(
            int analysisWindowDays,
            double analysisExpenseTotal,
            double baselineMonthlyAverage,
            double fixedChargesTotal,
            double incomeTotal,
            Map<TransactionCategory, Double> categoryTotals
    ) {
        Map<TransactionCategory, BigDecimal> totals = new EnumMap<>(TransactionCategory.class);
        categoryTotals.forEach((category, amount) -> totals.put(category, money(amount)));

        return ExpenseMetricsSnapshot.builder()
                .analysisWindowDays(analysisWindowDays)
                .analysisStartDate(ANALYSIS_END_DATE.minusDays(analysisWindowDays - 1L))
                .analysisEndDate(ANALYSIS_END_DATE)
                .analysisExpenseTotal(money(analysisExpenseTotal))
                .baselineMonthlyAverage(money(baselineMonthlyAverage))
                .analysisCategoryTotals(totals)
                .fixedChargesTotal(money(fixedChargesTotal))
                .analysisIncomeTotal(money(incomeTotal))
                .build();
    }

    private List<Transaction> incomeTransactions(
            double bucket0Amount,
            double bucket1Amount,
            double bucket2Amount,
            double bucket3Amount
    ) {
        return List.of(
                incomeTransaction(bucket0Amount, LocalDate.of(2026, 4, 1)),
                incomeTransaction(bucket1Amount, LocalDate.of(2026, 3, 1)),
                incomeTransaction(bucket2Amount, LocalDate.of(2026, 2, 1)),
                incomeTransaction(bucket3Amount, LocalDate.of(2026, 1, 1))
        ).stream()
                .filter(transaction -> transaction.getAmount() > 0.0d)
                .toList();
    }

    private Transaction incomeTransaction(double amount, LocalDate date) {
        return Transaction.builder()
                .description("income")
                .amount(amount)
                .date(date)
                .category(TransactionCategory.AUTRES)
                .type(TransactionType.REVENU)
                .build();
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2);
    }
}
