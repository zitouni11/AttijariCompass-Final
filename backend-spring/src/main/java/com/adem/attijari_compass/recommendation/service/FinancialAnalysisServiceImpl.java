package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.entity.FinancialGoal;
import com.adem.attijari_compass.entity.GoalStatus;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.repository.FinancialGoalRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialAnalysisServiceImpl implements FinancialAnalysisService {

    private static final List<TransactionCategory> FIXED_EXPENSE_CATEGORIES = List.of(
            TransactionCategory.LOGEMENT,
            TransactionCategory.OPERATEURS_TELEPHONIQUES,
            TransactionCategory.FACTURES,
            TransactionCategory.STEG_SONEDE,
            TransactionCategory.BANQUE,
            TransactionCategory.EDUCATION,
            TransactionCategory.TECHNOLOGIE
    );

    private static final Map<TransactionCategory, Double> SAVINGS_POTENTIAL_RATES = createSavingsPotentialRates();

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FinancialGoalRepository financialGoalRepository;

    @Override
    public FinancialInsightDto analyzeUserFinancials(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return analyzeUser(user);
    }

    @Override
    public FinancialInsightDto analyzeUserFinancials(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return analyzeUser(user);
    }

    private FinancialInsightDto analyzeUser(User user) {
        List<Transaction> transactions = transactionRepository.findAllByUserId(user.getId());
        List<FinancialGoal> goals = financialGoalRepository.findAllByUserId(user.getId());

        YearMonth analysisMonth = resolveAnalysisMonth(transactions);
        List<YearMonth> baselineMonths = List.of(
                analysisMonth.minusMonths(1),
                analysisMonth.minusMonths(2),
                analysisMonth.minusMonths(3)
        );

        List<Transaction> currentMonthTransactions = transactions.stream()
                .filter(transaction -> isInMonth(transaction, analysisMonth))
                .toList();

        double totalIncome = round(sumByType(currentMonthTransactions, TransactionType.REVENU));
        double totalExpenses = round(sumByType(currentMonthTransactions, TransactionType.DEPENSE));
        double remainingBalance = round(totalIncome - totalExpenses);
        double savingsAmount = remainingBalance;
        double savingsRate = totalIncome > 0.0 ? round((savingsAmount / totalIncome) * 100.0) : 0.0;

        double restaurantExpense = round(sumByCategories(currentMonthTransactions, List.of(
                TransactionCategory.RESTAURANT,
                TransactionCategory.CAFES,
                TransactionCategory.LIVRAISON
        )));
        double shoppingExpense = round(sumByCategory(currentMonthTransactions, TransactionCategory.SHOPPING));
        double transportExpense = round(sumByCategory(currentMonthTransactions, TransactionCategory.TRANSPORT));
        double fixedExpense = round(sumByCategories(currentMonthTransactions, FIXED_EXPENSE_CATEGORIES));
        double variableExpense = round(Math.max(0.0, totalExpenses - fixedExpense));

        double averageRestaurant3Months = round(averageMonthlyExpenseForCategories(transactions, baselineMonths, List.of(
                TransactionCategory.RESTAURANT,
                TransactionCategory.CAFES,
                TransactionCategory.LIVRAISON
        )));
        double averageShopping3Months = round(averageMonthlyExpenseForCategory(transactions, baselineMonths, TransactionCategory.SHOPPING));

        boolean restaurantOverspending = averageRestaurant3Months > 0.0
                && restaurantExpense > averageRestaurant3Months * 1.20;
        boolean shoppingOverspending = averageShopping3Months > 0.0
                && shoppingExpense > averageShopping3Months * 1.15;
        boolean savingsTooLow = totalIncome <= 0.0 || savingsRate < 10.0;

        GoalProjection goalProjection = buildGoalProjection(goals, transactions, analysisMonth, savingsAmount);
        AnomalyDetection anomalyDetection = detectAnomaly(transactions, analysisMonth);

        double possibleSavingsPotential = round(calculateSavingsPotential(currentMonthTransactions));

        boolean goodFinancialDiscipline = !restaurantOverspending
                && !shoppingOverspending
                && !savingsTooLow
                && !goalProjection.goalDelayed()
                && !anomalyDetection.detected()
                && totalIncome > 0.0
                && totalExpenses <= totalIncome
                && savingsRate >= 15.0;

        return FinancialInsightDto.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .savingsAmount(round(savingsAmount))
                .savingsRate(savingsRate)
                .remainingBalance(remainingBalance)
                .restaurantExpense(restaurantExpense)
                .shoppingExpense(shoppingExpense)
                .transportExpense(transportExpense)
                .fixedExpense(fixedExpense)
                .variableExpense(variableExpense)
                .averageRestaurant3Months(averageRestaurant3Months)
                .averageShopping3Months(averageShopping3Months)
                .restaurantOverspending(restaurantOverspending)
                .shoppingOverspending(shoppingOverspending)
                .savingsTooLow(savingsTooLow)
                .goalDelayed(goalProjection.goalDelayed())
                .anomalyDetected(anomalyDetection.detected())
                .goodFinancialDiscipline(goodFinancialDiscipline)
                .anomalyAmount(round(anomalyDetection.amount()))
                .requiredMonthlyContributionForGoal(round(goalProjection.requiredMonthlyContribution()))
                .currentMonthlyContribution(round(goalProjection.currentMonthlyContribution()))
                .possibleSavingsPotential(possibleSavingsPotential)
                .build();
    }

    private GoalProjection buildGoalProjection(
            List<FinancialGoal> goals,
            List<Transaction> transactions,
            YearMonth analysisMonth,
            double savingsAmount
    ) {
        Optional<FinancialGoal> activeGoal = goals.stream()
                .filter(Objects::nonNull)
                .filter(goal -> goal.getStatus() != GoalStatus.ATTEINT)
                .filter(goal -> goal.getTargetDate() != null && goal.getTargetDate().isAfter(LocalDate.now().minusDays(1)))
                .filter(goal -> safe(goal.getTargetAmount()) > safe(goal.getCurrentAmount()))
                .sorted((left, right) -> left.getTargetDate().compareTo(right.getTargetDate()))
                .findFirst();

        if (activeGoal.isEmpty()) {
            return new GoalProjection(0.0, Math.max(0.0, savingsAmount), false);
        }

        FinancialGoal goal = activeGoal.get();
        double remainingAmount = Math.max(0.0, safe(goal.getTargetAmount()) - safe(goal.getCurrentAmount()));
        long daysToTarget = Math.max(1L, ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate()));
        double monthsToTarget = Math.max(1.0, Math.ceil(daysToTarget / 30.0));
        double requiredMonthlyContribution = remainingAmount > 0.0
                ? round(remainingAmount / monthsToTarget)
                : 0.0;

        double currentMonthlyContribution = round(Math.max(0.0, savingsAmount));
        boolean goalDelayed = requiredMonthlyContribution > 0.0
                && currentMonthlyContribution + 0.01 < requiredMonthlyContribution;

        return new GoalProjection(requiredMonthlyContribution, currentMonthlyContribution, goalDelayed);
    }

    private AnomalyDetection detectAnomaly(List<Transaction> transactions, YearMonth analysisMonth) {
        List<Transaction> currentMonthExpenses = transactions.stream()
                .filter(transaction -> isInMonth(transaction, analysisMonth))
                .filter(transaction -> transaction.getType() == TransactionType.DEPENSE)
                .toList();

        List<Transaction> baselineExpenses = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.DEPENSE)
                .filter(transaction -> transaction.getDate() != null)
                .filter(transaction -> {
                    YearMonth month = YearMonth.from(transaction.getDate());
                    return month.isBefore(analysisMonth) && !month.isBefore(analysisMonth.minusMonths(3));
                })
                .toList();

        if (baselineExpenses.isEmpty() || currentMonthExpenses.isEmpty()) {
            return new AnomalyDetection(false, 0.0);
        }

        double globalAverage = averageAmount(baselineExpenses);
        double globalStdDev = standardDeviationAmounts(baselineExpenses, globalAverage);

        Map<TransactionCategory, List<Transaction>> baselineByCategory = baselineExpenses.stream()
                .filter(transaction -> transaction.getCategory() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        Transaction::getCategory,
                        () -> new EnumMap<>(TransactionCategory.class),
                        java.util.stream.Collectors.toList()
                ));

        double largestAnomaly = 0.0;

        for (Transaction transaction : currentMonthExpenses) {
            List<Transaction> categoryHistory = baselineByCategory.getOrDefault(transaction.getCategory(), List.of());
            double categoryAverage = categoryHistory.isEmpty() ? globalAverage : averageAmount(categoryHistory);
            double categoryStdDev = categoryHistory.size() < 2
                    ? globalStdDev
                    : standardDeviationAmounts(categoryHistory, categoryAverage);

            double threshold = Math.max(
                    categoryAverage + (categoryStdDev * 2.0),
                    Math.max(categoryAverage * 2.25, globalAverage * 2.00)
            );

            if (safe(transaction.getAmount()) > threshold && safe(transaction.getAmount()) > 150.0) {
                largestAnomaly = Math.max(largestAnomaly, safe(transaction.getAmount()));
            }
        }

        return new AnomalyDetection(largestAnomaly > 0.0, round(largestAnomaly));
    }

    private double calculateSavingsPotential(List<Transaction> currentMonthTransactions) {
        // TODO adapt with dedicated analytical repository projections if transaction volume grows significantly.
        return SAVINGS_POTENTIAL_RATES.entrySet().stream()
                .mapToDouble(entry -> sumByCategory(currentMonthTransactions, entry.getKey()) * entry.getValue())
                .sum();
    }

    private YearMonth resolveAnalysisMonth(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return YearMonth.now();
        }

        boolean hasCurrentMonthTransactions = transactions.stream()
                .map(Transaction::getDate)
                .filter(Objects::nonNull)
                .map(YearMonth::from)
                .anyMatch(month -> month.equals(YearMonth.now()));

        if (hasCurrentMonthTransactions) {
            return YearMonth.now();
        }

        return transactions.stream()
                .map(Transaction::getDate)
                .filter(Objects::nonNull)
                .map(YearMonth::from)
                .max(YearMonth::compareTo)
                .orElse(YearMonth.now());
    }

    private boolean isInMonth(Transaction transaction, YearMonth month) {
        return transaction.getDate() != null && YearMonth.from(transaction.getDate()).equals(month);
    }

    private double averageMonthlyExpenseForCategory(
            List<Transaction> transactions,
            List<YearMonth> months,
            TransactionCategory category
    ) {
        return averageMonthlyAmount(transactions, months, category, TransactionType.DEPENSE);
    }

    private double averageMonthlyExpenseForCategories(
            List<Transaction> transactions,
            List<YearMonth> months,
            List<TransactionCategory> categories
    ) {
        return months.stream()
                .mapToDouble(month -> categories.stream()
                        .mapToDouble(category -> averageMonthlyAmount(transactions, List.of(month), category, TransactionType.DEPENSE))
                        .sum())
                .average()
                .orElse(0.0);
    }

    private double averageMonthlyAmountForCategory(
            List<Transaction> transactions,
            List<YearMonth> months,
            TransactionCategory category
    ) {
        return averageMonthlyAmount(transactions, months, category, null);
    }

    private double averageMonthlyAmount(
            List<Transaction> transactions,
            List<YearMonth> months,
            TransactionCategory category,
            TransactionType type
    ) {
        if (months.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (YearMonth month : months) {
            total += transactions.stream()
                    .filter(transaction -> isInMonth(transaction, month))
                    .filter(transaction -> transaction.getCategory() == category)
                    .filter(transaction -> type == null || transaction.getType() == type)
                    .mapToDouble(transaction -> safe(transaction.getAmount()))
                    .sum();
        }

        return total / months.size();
    }

    private double sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .mapToDouble(transaction -> safe(transaction.getAmount()))
                .sum();
    }

    private double sumByCategory(List<Transaction> transactions, TransactionCategory category) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.DEPENSE)
                .filter(transaction -> transaction.getCategory() == category)
                .mapToDouble(transaction -> safe(transaction.getAmount()))
                .sum();
    }

    private double sumByCategories(List<Transaction> transactions, List<TransactionCategory> categories) {
        return categories.stream()
                .mapToDouble(category -> sumByCategory(transactions, category))
                .sum();
    }

    private double averageAmount(List<Transaction> transactions) {
        return transactions.stream()
                .mapToDouble(transaction -> safe(transaction.getAmount()))
                .average()
                .orElse(0.0);
    }

    private double standardDeviationAmounts(List<Transaction> transactions, double average) {
        if (transactions.size() < 2) {
            return 0.0;
        }

        double variance = transactions.stream()
                .mapToDouble(transaction -> Math.pow(safe(transaction.getAmount()) - average, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static Map<TransactionCategory, Double> createSavingsPotentialRates() {
        EnumMap<TransactionCategory, Double> rates = new EnumMap<>(TransactionCategory.class);
        rates.put(TransactionCategory.RESTAURANT, 0.30);
        rates.put(TransactionCategory.CAFES, 0.30);
        rates.put(TransactionCategory.LIVRAISON, 0.30);
        rates.put(TransactionCategory.SHOPPING, 0.25);
        rates.put(TransactionCategory.TECHNOLOGIE, 0.25);
        rates.put(TransactionCategory.DIVERTISSEMENT, 0.20);
        rates.put(TransactionCategory.HOTEL, 0.30);
        rates.put(TransactionCategory.VOYAGE, 0.20);
        rates.put(TransactionCategory.TRANSPORT, 0.10);
        rates.put(TransactionCategory.FACTURES, 0.08);
        return Map.copyOf(rates);
    }

    private record GoalProjection(
            double requiredMonthlyContribution,
            double currentMonthlyContribution,
            boolean goalDelayed
    ) {
    }

    private record AnomalyDetection(
            boolean detected,
            double amount
    ) {
    }
}
