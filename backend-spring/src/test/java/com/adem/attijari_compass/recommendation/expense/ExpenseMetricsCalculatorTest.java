package com.adem.attijari_compass.recommendation.expense;

import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExpenseMetricsCalculatorTest {

    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final ExpenseMetricsCalculator calculator = new ExpenseMetricsCalculator(transactionRepository);

    @Test
    void shouldCalculateSnapshotFromThirtyDayWindowAndThreeMonthBaseline() {
        Long userId = 1L;
        LocalDate analysisEndDate = LocalDate.of(2026, 4, 11);

        when(transactionRepository.findAllByUserIdAndDateBetween(userId, LocalDate.of(2026, 1, 1), analysisEndDate))
                .thenReturn(List.of(
                        transaction(TransactionType.DEPENSE, TransactionCategory.CAFES, 100.0d, LocalDate.of(2026, 4, 2)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.HOTEL, 500.0d, LocalDate.of(2026, 4, 5)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.STEG_SONEDE, 150.0d, LocalDate.of(2026, 4, 8)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.ALIMENTATION, 80.0d, LocalDate.of(2026, 3, 20)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.CAFES, 60.0d, LocalDate.of(2026, 3, 25)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.AUTRES, 999.0d, LocalDate.of(2026, 4, 9)),
                        transaction(TransactionType.DEPENSE, null, 50.0d, LocalDate.of(2026, 3, 28)),
                        transaction(TransactionType.REVENU, TransactionCategory.AUTRES, 2000.0d, LocalDate.of(2026, 4, 1)),
                        transaction(TransactionType.REVENU, TransactionCategory.BANQUE, 300.0d, LocalDate.of(2026, 3, 30)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.CAFES, 90.0d, LocalDate.of(2026, 2, 11)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.ALIMENTATION, 120.0d, LocalDate.of(2026, 1, 9)),
                        transaction(TransactionType.REVENU, TransactionCategory.AUTRES, 1800.0d, LocalDate.of(2026, 3, 5))
                ));

        ExpenseMetricsSnapshot snapshot = calculator.calculate(userId, analysisEndDate);

        assertEquals(30, snapshot.getAnalysisWindowDays());
        assertEquals(LocalDate.of(2026, 3, 13), snapshot.getAnalysisStartDate());
        assertEquals(analysisEndDate, snapshot.getAnalysisEndDate());
        assertEquals(new BigDecimal("890.00"), snapshot.getAnalysisExpenseTotal());
        assertEquals(new BigDecimal("116.67"), snapshot.getBaselineMonthlyAverage());
        assertEquals(new BigDecimal("160.00"), snapshot.getAnalysisCategoryTotals().get(TransactionCategory.CAFES));
        assertEquals(new BigDecimal("500.00"), snapshot.getAnalysisCategoryTotals().get(TransactionCategory.HOTEL));
        assertEquals(new BigDecimal("150.00"), snapshot.getAnalysisCategoryTotals().get(TransactionCategory.STEG_SONEDE));
        assertEquals(new BigDecimal("50.00"), snapshot.getBaselineCategoryAverages().get(TransactionCategory.CAFES));
        assertEquals(new BigDecimal("66.67"), snapshot.getBaselineCategoryAverages().get(TransactionCategory.ALIMENTATION));
        assertEquals(Long.valueOf(2L), snapshot.getAnalysisCategoryCounts().get(TransactionCategory.CAFES));
        assertEquals(Long.valueOf(1L), snapshot.getAnalysisCategoryCounts().get(TransactionCategory.HOTEL));
        assertEquals(new BigDecimal("650.00"), snapshot.getFixedChargesTotal());
        assertEquals(new BigDecimal("2300.00"), snapshot.getAnalysisIncomeTotal());
        assertFalse(snapshot.getAnalysisCategoryTotals().containsKey(TransactionCategory.AUTRES));
    }

    @Test
    void shouldReturnDefaultSnapshotWhenUserIdIsNull() {
        ExpenseMetricsSnapshot snapshot = calculator.calculate(null, LocalDate.of(2026, 4, 11));

        assertEquals(BigDecimal.ZERO, snapshot.getAnalysisExpenseTotal());
        assertEquals(BigDecimal.ZERO, snapshot.getBaselineMonthlyAverage());
        assertEquals(BigDecimal.ZERO, snapshot.getFixedChargesTotal());
        assertEquals(BigDecimal.ZERO, snapshot.getAnalysisIncomeTotal());
        assertEquals(0, snapshot.getAnalysisCategoryTotals().size());
        assertEquals(0, snapshot.getBaselineCategoryAverages().size());
        assertEquals(0, snapshot.getAnalysisCategoryCounts().size());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldFallbackToNinetyDayWindowWhenThirtyDaySampleIsTooSmall() {
        Long userId = 2L;
        LocalDate analysisEndDate = LocalDate.of(2026, 4, 11);

        when(transactionRepository.findAllByUserIdAndDateBetween(userId, LocalDate.of(2026, 1, 1), analysisEndDate))
                .thenReturn(List.of(
                        transaction(TransactionType.DEPENSE, TransactionCategory.ALIMENTATION, 80.0d, LocalDate.of(2026, 4, 2)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.ALIMENTATION, 60.0d, LocalDate.of(2026, 3, 25)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.ALIMENTATION, 55.0d, LocalDate.of(2026, 3, 20)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.CAFES, 120.0d, LocalDate.of(2026, 2, 20)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.TRANSPORT, 90.0d, LocalDate.of(2026, 2, 10)),
                        transaction(TransactionType.DEPENSE, TransactionCategory.HOTEL, 500.0d, LocalDate.of(2026, 1, 25)),
                        transaction(TransactionType.REVENU, TransactionCategory.AUTRES, 1500.0d, LocalDate.of(2026, 4, 1))
                ));

        ExpenseMetricsSnapshot snapshot = calculator.calculate(userId, analysisEndDate);

        assertEquals(90, snapshot.getAnalysisWindowDays());
        assertEquals(LocalDate.of(2026, 1, 12), snapshot.getAnalysisStartDate());
        assertEquals(analysisEndDate, snapshot.getAnalysisEndDate());
        assertEquals(new BigDecimal("905.00"), snapshot.getAnalysisExpenseTotal());
        assertEquals(Long.valueOf(3L), snapshot.getAnalysisCategoryCounts().get(TransactionCategory.ALIMENTATION));
        assertEquals(new BigDecimal("1500.00"), snapshot.getAnalysisIncomeTotal());
    }

    private Transaction transaction(
            TransactionType type,
            TransactionCategory category,
            double amount,
            LocalDate date
    ) {
        return Transaction.builder()
                .description("test")
                .amount(amount)
                .date(date)
                .category(category)
                .type(type)
                .build();
    }
}
