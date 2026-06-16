package com.adem.attijari_compass.recommendation.expense;

import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExpenseMetricsCalculator {

    static final int DEFAULT_ANALYSIS_WINDOW_DAYS = 30;
    static final int FALLBACK_ANALYSIS_WINDOW_DAYS = 90;

    private static final int MIN_ELIGIBLE_EXPENSES = 5;
    private static final int MIN_EXPENSE_CATEGORIES = 2;
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal BASELINE_MONTH_DIVISOR =
            BigDecimal.valueOf(ExpenseRecommendationThresholds.MIN_BASELINE_MONTHS);

    private final TransactionRepository transactionRepository;

    public ExpenseMetricsSnapshot calculate(Long userId) {
        return calculate(userId, LocalDate.now());
    }

    ExpenseMetricsSnapshot calculate(Long userId, LocalDate analysisEndDate) {
        if (userId == null) {
            return ExpenseMetricsSnapshot.builder().build();
        }

        LocalDate resolvedAnalysisEndDate = analysisEndDate != null ? analysisEndDate : LocalDate.now();
        LocalDate baselineStartDate = YearMonth.from(resolvedAnalysisEndDate)
                .minusMonths(ExpenseRecommendationThresholds.MIN_BASELINE_MONTHS)
                .atDay(1);
        LocalDate fallbackWindowStartDate = resolvedAnalysisEndDate.minusDays(FALLBACK_ANALYSIS_WINDOW_DAYS - 1L);
        LocalDate loadStartDate = baselineStartDate.isBefore(fallbackWindowStartDate)
                ? baselineStartDate
                : fallbackWindowStartDate;

        List<Transaction> loadedTransactions = loadTransactions(userId, loadStartDate, resolvedAnalysisEndDate);
        AnalysisWindow analysisWindow = resolveAnalysisWindow(loadedTransactions, resolvedAnalysisEndDate);
        List<Transaction> analysisTransactions = filterTransactionsBetween(
                loadedTransactions,
                analysisWindow.startDate(),
                analysisWindow.endDate()
        );
        List<Transaction> baselineTransactions = filterTransactionsForBaseline(
                loadedTransactions,
                analysisWindow.endDate()
        );

        Map<TransactionCategory, BigDecimal> analysisCategoryTotals =
                calculateAnalysisCategoryTotals(analysisTransactions);
        Map<TransactionCategory, Long> analysisCategoryCounts =
                calculateAnalysisCategoryCounts(analysisTransactions);
        BigDecimal analysisExpenseTotal = sumEligibleExpenses(analysisTransactions);
        BigDecimal analysisIncomeTotal = calculateAnalysisIncomeTotal(analysisTransactions);
        BigDecimal fixedChargesTotal = calculateFixedChargesTotal(analysisCategoryTotals);

        log.info(
                "Expense analysis resolved: userId={}, analysisWindowDays={}, analysisStartDate={}, analysisEndDate={}, eligibleExpenseCount={}, expenseCategoryCount={}, analysisExpenseTotal={}",
                userId,
                analysisWindow.windowDays(),
                analysisWindow.startDate(),
                analysisWindow.endDate(),
                countEligibleExpenses(analysisTransactions),
                countExpenseCategories(analysisTransactions),
                analysisExpenseTotal
        );

        return ExpenseMetricsSnapshot.builder()
                .analysisWindowDays(analysisWindow.windowDays())
                .analysisStartDate(analysisWindow.startDate())
                .analysisEndDate(analysisWindow.endDate())
                .analysisExpenseTotal(analysisExpenseTotal)
                .analysisCategoryTotals(analysisCategoryTotals)
                .analysisCategoryCounts(analysisCategoryCounts)
                .baselineMonthlyAverage(calculateBaselineMonthlyAverage(baselineTransactions))
                .baselineCategoryAverages(calculateBaselineCategoryAverages(baselineTransactions))
                .fixedChargesTotal(fixedChargesTotal)
                .analysisIncomeTotal(analysisIncomeTotal)
                .build();
    }

    private List<Transaction> loadTransactions(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findAllByUserIdAndDateBetween(
                userId,
                startDate,
                endDate
        );
        return transactions != null ? transactions : List.of();
    }

    private List<Transaction> filterTransactionsBetween(
            List<Transaction> transactions,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction == null || transaction.getDate() == null) {
                continue;
            }
            if (!transaction.getDate().isBefore(startDate) && !transaction.getDate().isAfter(endDate)) {
                filtered.add(transaction);
            }
        }
        return filtered;
    }

    private long countEligibleExpenses(List<Transaction> transactions) {
        long count = 0L;
        for (Transaction transaction : transactions) {
            if (isEligibleExpense(transaction)) {
                count++;
            }
        }
        return count;
    }

    private int countExpenseCategories(List<Transaction> transactions) {
        Set<TransactionCategory> categories = new HashSet<>();
        for (Transaction transaction : transactions) {
            if (isEligibleExpense(transaction) && transaction.getCategory() != null) {
                categories.add(transaction.getCategory());
            }
        }
        return categories.size();
    }

    private AnalysisWindow resolveAnalysisWindow(List<Transaction> transactions, LocalDate analysisEndDate) {
        LocalDate defaultWindowStartDate = analysisEndDate.minusDays(DEFAULT_ANALYSIS_WINDOW_DAYS - 1L);
        List<Transaction> defaultWindowTransactions = filterTransactionsBetween(
                transactions,
                defaultWindowStartDate,
                analysisEndDate
        );

        long eligibleExpenseCount = countEligibleExpenses(defaultWindowTransactions);
        int expenseCategoryCount = countExpenseCategories(defaultWindowTransactions);
        boolean fallbackToExtendedWindow = eligibleExpenseCount < MIN_ELIGIBLE_EXPENSES
                || expenseCategoryCount < MIN_EXPENSE_CATEGORIES;

        if (!fallbackToExtendedWindow) {
            return new AnalysisWindow(DEFAULT_ANALYSIS_WINDOW_DAYS, defaultWindowStartDate, analysisEndDate);
        }

        LocalDate fallbackWindowStartDate = analysisEndDate.minusDays(FALLBACK_ANALYSIS_WINDOW_DAYS - 1L);
        return new AnalysisWindow(FALLBACK_ANALYSIS_WINDOW_DAYS, fallbackWindowStartDate, analysisEndDate);
    }

    private List<Transaction> filterTransactionsForBaseline(List<Transaction> transactions, LocalDate analysisEndDate) {
        YearMonth analysisMonth = YearMonth.from(analysisEndDate);
        YearMonth baselineStartMonth = analysisMonth.minusMonths(ExpenseRecommendationThresholds.MIN_BASELINE_MONTHS);
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction transaction : transactions) {
            if (transaction == null || transaction.getDate() == null) {
                continue;
            }

            YearMonth transactionMonth = YearMonth.from(transaction.getDate());
            if (transactionMonth.isBefore(analysisMonth) && !transactionMonth.isBefore(baselineStartMonth)) {
                filtered.add(transaction);
            }
        }

        return filtered;
    }

    private Map<TransactionCategory, BigDecimal> calculateAnalysisCategoryTotals(List<Transaction> transactions) {
        EnumMap<TransactionCategory, BigDecimal> totals = new EnumMap<>(TransactionCategory.class);

        for (Transaction transaction : transactions) {
            if (!isEligibleExpense(transaction)) {
                continue;
            }
            totals.merge(transaction.getCategory(), toPositiveAmount(transaction.getAmount()), BigDecimal::add);
        }

        return normalizeMoneyMap(totals);
    }

    private Map<TransactionCategory, Long> calculateAnalysisCategoryCounts(List<Transaction> transactions) {
        EnumMap<TransactionCategory, Long> counts = new EnumMap<>(TransactionCategory.class);

        for (Transaction transaction : transactions) {
            if (!isEligibleExpense(transaction)) {
                continue;
            }
            counts.merge(transaction.getCategory(), 1L, Long::sum);
        }

        return counts;
    }

    private BigDecimal calculateBaselineMonthlyAverage(List<Transaction> baselineTransactions) {
        return divideByBaselineMonths(sumEligibleExpenses(baselineTransactions));
    }

    private Map<TransactionCategory, BigDecimal> calculateBaselineCategoryAverages(List<Transaction> baselineTransactions) {
        EnumMap<TransactionCategory, BigDecimal> totals = new EnumMap<>(TransactionCategory.class);

        for (Transaction transaction : baselineTransactions) {
            if (!isEligibleExpense(transaction)) {
                continue;
            }
            totals.merge(transaction.getCategory(), toPositiveAmount(transaction.getAmount()), BigDecimal::add);
        }

        EnumMap<TransactionCategory, BigDecimal> averages = new EnumMap<>(TransactionCategory.class);
        for (Map.Entry<TransactionCategory, BigDecimal> entry : totals.entrySet()) {
            averages.put(entry.getKey(), divideByBaselineMonths(entry.getValue()));
        }

        return averages;
    }

    private BigDecimal calculateFixedChargesTotal(Map<TransactionCategory, BigDecimal> analysisCategoryTotals) {
        BigDecimal housingTotal = analysisCategoryTotals.getOrDefault(TransactionCategory.LOGEMENT, zeroMoney());
        BigDecimal operatorsTotal = analysisCategoryTotals.getOrDefault(TransactionCategory.OPERATEURS_TELEPHONIQUES, zeroMoney());
        BigDecimal invoicesTotal = analysisCategoryTotals.getOrDefault(TransactionCategory.FACTURES, zeroMoney());
        BigDecimal utilitiesTotal = analysisCategoryTotals.getOrDefault(TransactionCategory.STEG_SONEDE, zeroMoney());
        BigDecimal bankingTotal = analysisCategoryTotals.getOrDefault(TransactionCategory.BANQUE, zeroMoney());
        return normalizeMoney(housingTotal.add(operatorsTotal).add(invoicesTotal).add(utilitiesTotal).add(bankingTotal));
    }

    private BigDecimal calculateAnalysisIncomeTotal(List<Transaction> analysisTransactions) {
        BigDecimal total = zeroMoney();

        for (Transaction transaction : analysisTransactions) {
            if (transaction == null || transaction.getType() != TransactionType.REVENU) {
                continue;
            }
            total = total.add(toPositiveAmount(transaction.getAmount()));
        }

        return normalizeMoney(total);
    }

    private BigDecimal sumEligibleExpenses(List<Transaction> transactions) {
        BigDecimal total = zeroMoney();

        for (Transaction transaction : transactions) {
            if (!isEligibleExpense(transaction)) {
                continue;
            }
            total = total.add(toPositiveAmount(transaction.getAmount()));
        }

        return normalizeMoney(total);
    }

    private boolean isEligibleExpense(Transaction transaction) {
        return transaction != null
                && transaction.getType() == TransactionType.DEPENSE
                && ExpenseCategoryPolicy.isExpenseEligible(transaction.getCategory());
    }

    private BigDecimal divideByBaselineMonths(BigDecimal total) {
        return normalizeMoney(total.divide(BASELINE_MONTH_DIVISOR, MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private Map<TransactionCategory, BigDecimal> normalizeMoneyMap(Map<TransactionCategory, BigDecimal> values) {
        EnumMap<TransactionCategory, BigDecimal> normalized = new EnumMap<>(TransactionCategory.class);
        for (Map.Entry<TransactionCategory, BigDecimal> entry : values.entrySet()) {
            normalized.put(entry.getKey(), normalizeMoney(entry.getValue()));
        }
        return normalized;
    }

    private BigDecimal toPositiveAmount(Double amount) {
        if (amount == null) {
            return zeroMoney();
        }
        return BigDecimal.valueOf(amount).abs();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return zeroMoney();
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record AnalysisWindow(
            int windowDays,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
