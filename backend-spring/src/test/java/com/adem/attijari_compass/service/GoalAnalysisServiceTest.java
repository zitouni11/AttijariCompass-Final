package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.goal.GoalAnalysisResponse;
import com.adem.attijari_compass.entity.FinancialGoal;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalAnalysisServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GoalAnalysisService goalAnalysisService;

    @Test
    void shouldReturnRobustAnalysisWhenNoTransactionsAvailable() {
        FinancialGoal goal = goal(1_000.0, 100.0, LocalDate.now().plusMonths(6));
        when(transactionRepository.findAllByUserId(1L)).thenReturn(List.of());

        GoalAnalysisResponse response = goalAnalysisService.getGoalAnalysis(goal);

        assertFalse(response.getEnoughData());
        assertEquals(1, response.getMonthsAnalyzed());
        assertEquals(0, response.getTransactionCount());
        assertEquals(0.0, response.getAverageMonthlyIncome());
        assertEquals(0.0, response.getFixedExpenses());
        assertEquals(0.0, response.getCompressibleExpenses());
        assertEquals(50.0, response.getPrudentSavingCapacity());
        assertEquals(75.0, response.getBalancedSavingCapacity());
        assertEquals(100.0, response.getAggressiveSavingCapacity());
        assertTrue(response.getBlockingCategories().isEmpty());
        assertTrue(response.getAnalysisMessage().contains("Historique insuffisant"));
    }

    @Test
    void shouldFlagAnalysisAsInsufficientWhenHistoryIsTooShort() {
        FinancialGoal goal = goal(4_000.0, 300.0, LocalDate.now().plusMonths(5));
        when(transactionRepository.findAllByUserId(1L)).thenReturn(List.of(
                income(2_000.0, LocalDate.now().minusDays(10)),
                expense(500.0, TransactionCategory.CAFES, LocalDate.now().minusDays(8))
        ));

        GoalAnalysisResponse response = goalAnalysisService.getGoalAnalysis(goal);

        assertFalse(response.getEnoughData());
        assertEquals(1, response.getMonthsAnalyzed());
        assertEquals(2, response.getTransactionCount());
        assertEquals(2_000.0, response.getAverageMonthlyIncome());
        assertEquals(500.0, response.getAverageMonthlyExpenses());
        assertEquals(0.0, response.getFixedExpenses());
        assertEquals(500.0, response.getCompressibleExpenses());
        assertTrue(response.getBalancedSavingCapacity() >= response.getPrudentSavingCapacity());
        assertTrue(response.getAnalysisMessage().contains("donnees limitees"));
    }

    @Test
    void shouldDetectBlockingCategoriesAndComputeSavingsCapacities() {
        FinancialGoal goal = goal(8_000.0, 1_000.0, LocalDate.now().plusMonths(8));
        when(transactionRepository.findAllByUserId(1L)).thenReturn(List.of(
                income(5_000.0, monthDate(2, 5)),
                expense(1_500.0, TransactionCategory.HOTEL, monthDate(2, 6)),
                expense(600.0, TransactionCategory.ALIMENTATION, monthDate(2, 8)),
                expense(450.0, TransactionCategory.CAFES, monthDate(2, 10)),
                expense(400.0, TransactionCategory.SHOPPING, monthDate(2, 12)),
                expense(250.0, TransactionCategory.DIVERTISSEMENT, monthDate(2, 14)),
                expense(300.0, TransactionCategory.TRANSPORT, monthDate(2, 18)),
                expense(200.0, TransactionCategory.STEG_SONEDE, monthDate(2, 21)),

                income(5_000.0, monthDate(1, 5)),
                expense(1_500.0, TransactionCategory.HOTEL, monthDate(1, 6)),
                expense(650.0, TransactionCategory.ALIMENTATION, monthDate(1, 8)),
                expense(500.0, TransactionCategory.CAFES, monthDate(1, 10)),
                expense(350.0, TransactionCategory.SHOPPING, monthDate(1, 12)),
                expense(200.0, TransactionCategory.DIVERTISSEMENT, monthDate(1, 14)),
                expense(320.0, TransactionCategory.TRANSPORT, monthDate(1, 18)),
                expense(220.0, TransactionCategory.STEG_SONEDE, monthDate(1, 21)),

                income(5_000.0, monthDate(0, 5)),
                expense(1_500.0, TransactionCategory.HOTEL, monthDate(0, 6)),
                expense(700.0, TransactionCategory.ALIMENTATION, monthDate(0, 8)),
                expense(550.0, TransactionCategory.CAFES, monthDate(0, 10)),
                expense(300.0, TransactionCategory.SHOPPING, monthDate(0, 12)),
                expense(220.0, TransactionCategory.DIVERTISSEMENT, monthDate(0, 14)),
                expense(280.0, TransactionCategory.TRANSPORT, monthDate(0, 18)),
                expense(210.0, TransactionCategory.STEG_SONEDE, monthDate(0, 21))
        ));

        GoalAnalysisResponse response = goalAnalysisService.getGoalAnalysis(goal);

        assertTrue(response.getEnoughData());
        assertEquals(3, response.getMonthsAnalyzed());
        assertEquals(24, response.getTransactionCount());
        assertEquals(5_000.0, response.getAverageMonthlyIncome());
        assertEquals(3733.33, response.getAverageMonthlyExpenses());
        assertEquals(1710.0, response.getFixedExpenses());
        assertEquals(1710.0, response.getEstimatedFixedExpenses());
        assertEquals(2570.0, response.getEstimatedEssentialExpenses());
        assertEquals(1073.33, response.getCompressibleExpenses());
        assertEquals(610.5, response.getEstimatedCompressibleExpenses());
        assertTrue(response.getPrudentSavingCapacity() > 1_300.0);
        assertTrue(response.getBalancedSavingCapacity() > response.getPrudentSavingCapacity());
        assertTrue(response.getAggressiveSavingCapacity() > response.getBalancedSavingCapacity());
        assertEquals(TransactionCategory.CAFES, response.getBlockingCategories().get(0).getCategory());
        assertEquals("Cafes", response.getBlockingCategories().get(0).getCategoryLabel());
        assertTrue(response.getBlockingCategories().get(0).getDisplayLabel().contains("Priorite"));
        assertEquals(TransactionCategory.SHOPPING, response.getBlockingCategories().get(1).getCategory());
    }

    @Test
    void shouldHandlePartialTransactionValuesWithoutFailing() {
        FinancialGoal goal = goal(2_000.0, 100.0, LocalDate.now().plusMonths(4));
        when(transactionRepository.findAllByUserId(1L)).thenReturn(List.of(
                Transaction.builder()
                        .amount(null)
                        .date(LocalDate.now().minusDays(20))
                        .type(TransactionType.REVENU)
                        .category(TransactionCategory.AUTRES)
                        .build(),
                Transaction.builder()
                        .amount(300.0)
                        .date(LocalDate.now().minusDays(18))
                        .type(TransactionType.DEPENSE)
                        .category(TransactionCategory.AUTRES)
                        .build(),
                Transaction.builder()
                        .amount(150.0)
                        .date(null)
                        .type(TransactionType.DEPENSE)
                        .category(TransactionCategory.CAFES)
                        .build()
        ));

        GoalAnalysisResponse response = goalAnalysisService.getGoalAnalysis(goal);

        assertNotNull(response);
        assertEquals(2, response.getTransactionCount());
        assertEquals(0.0, response.getAverageMonthlyIncome());
        assertEquals(300.0, response.getAverageMonthlyExpenses());
        assertEquals(0.0, response.getFixedExpenses());
        assertEquals(0.0, response.getCompressibleExpenses());
        assertFalse(response.getBlockingCategories().isEmpty());
        assertEquals(TransactionCategory.AUTRES, response.getBlockingCategories().get(0).getCategory());
        assertEquals("Autres depenses", response.getBlockingCategories().get(0).getCategoryLabel());
    }

    @Test
    void shouldUseLatestAvailableTransactionsWhenHistoryIsOld() {
        FinancialGoal goal = goal(5_000.0, 500.0, LocalDate.now().plusMonths(6));
        LocalDate anchor = LocalDate.now().minusMonths(8);
        when(transactionRepository.findAllByUserId(1L)).thenReturn(List.of(
                income(4_000.0, anchor.withDayOfMonth(5)),
                expense(1_000.0, TransactionCategory.HOTEL, anchor.withDayOfMonth(6)),
                expense(450.0, TransactionCategory.CAFES, anchor.withDayOfMonth(10)),
                income(4_200.0, anchor.plusMonths(1).withDayOfMonth(5)),
                expense(1_050.0, TransactionCategory.HOTEL, anchor.plusMonths(1).withDayOfMonth(6)),
                expense(500.0, TransactionCategory.CAFES, anchor.plusMonths(1).withDayOfMonth(10))
        ));

        GoalAnalysisResponse response = goalAnalysisService.getGoalAnalysis(goal);

        assertEquals(2, response.getMonthsAnalyzed());
        assertEquals(6, response.getTransactionCount());
        assertEquals(4100.0, response.getAverageMonthlyIncome());
        assertEquals(1025.0, response.getFixedExpenses());
        assertEquals(475.0, response.getCompressibleExpenses());
        assertTrue(response.getBalancedSavingCapacity() > 0.0);
        assertFalse(response.getBlockingCategories().isEmpty());
        assertEquals(TransactionCategory.CAFES, response.getBlockingCategories().get(0).getCategory());
        assertEquals("Cafes", response.getBlockingCategories().get(0).getCategoryLabel());
    }

    @Test
    void shouldApplyFallbackCapacitiesAndDetectBlockingCategoriesInDeficitContext() {
        FinancialGoal goal = goal(4_500.0, 100.0, LocalDate.now().plusMonths(6));
        when(transactionRepository.findAllByUserId(1L)).thenReturn(List.of(
                income(1_800.0, monthDate(2, 5)),
                expense(1_000.0, TransactionCategory.HOTEL, monthDate(2, 6)),
                expense(450.0, TransactionCategory.ALIMENTATION, monthDate(2, 8)),
                expense(250.0, TransactionCategory.STEG_SONEDE, monthDate(2, 10)),
                expense(350.0, TransactionCategory.CAFES, monthDate(2, 12)),

                income(1_850.0, monthDate(1, 5)),
                expense(1_000.0, TransactionCategory.HOTEL, monthDate(1, 6)),
                expense(420.0, TransactionCategory.ALIMENTATION, monthDate(1, 8)),
                expense(260.0, TransactionCategory.STEG_SONEDE, monthDate(1, 10)),
                expense(300.0, TransactionCategory.CAFES, monthDate(1, 12)),

                income(1_900.0, monthDate(0, 5)),
                expense(1_050.0, TransactionCategory.HOTEL, monthDate(0, 6)),
                expense(460.0, TransactionCategory.ALIMENTATION, monthDate(0, 8)),
                expense(240.0, TransactionCategory.STEG_SONEDE, monthDate(0, 10)),
                expense(320.0, TransactionCategory.CAFES, monthDate(0, 12))
        ));

        GoalAnalysisResponse response = goalAnalysisService.getGoalAnalysis(goal);

        assertTrue(response.getAverageMonthlyExpenses() > response.getAverageMonthlyIncome());
        assertEquals(1266.67, response.getFixedExpenses());
        assertEquals(323.33, response.getCompressibleExpenses());
        assertEquals(50.0, response.getPrudentSavingCapacity());
        assertEquals(75.0, response.getBalancedSavingCapacity());
        assertEquals(100.0, response.getAggressiveSavingCapacity());
        assertFalse(response.getBlockingCategories().isEmpty());
        assertTrue(response.getBlockingCategories().stream().allMatch(category -> category.getCategoryLabel() != null));
        assertTrue(response.getBlockingCategories().stream().allMatch(category -> category.getSeverityLabel() != null));
        assertTrue(response.getBlockingCategories().stream()
                .anyMatch(category -> category.getCategory() == TransactionCategory.HOTEL
                        || category.getCategory() == TransactionCategory.ALIMENTATION
                        || category.getCategory() == TransactionCategory.CAFES));
    }

    private FinancialGoal goal(double targetAmount, double currentAmount, LocalDate targetDate) {
        return FinancialGoal.builder()
                .id(10L)
                .targetAmount(targetAmount)
                .currentAmount(currentAmount)
                .targetDate(targetDate)
                .user(User.builder().id(1L).email("goal@test.com").build())
                .build();
    }

    private Transaction income(double amount, LocalDate date) {
        return Transaction.builder()
                .amount(amount)
                .date(date)
                .type(TransactionType.REVENU)
                .category(TransactionCategory.AUTRES)
                .description("income")
                .build();
    }

    private Transaction expense(double amount, TransactionCategory category, LocalDate date) {
        return Transaction.builder()
                .amount(amount)
                .date(date)
                .type(TransactionType.DEPENSE)
                .category(category)
                .description("expense")
                .build();
    }

    private LocalDate monthDate(int monthsAgo, int dayOfMonth) {
        return LocalDate.now().minusMonths(monthsAgo).withDayOfMonth(dayOfMonth);
    }
}
