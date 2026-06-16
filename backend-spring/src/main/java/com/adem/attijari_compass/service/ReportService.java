package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.budget.BudgetAlertResponse;
import com.adem.attijari_compass.dto.report.ReportCashBreakdownResponse;
import com.adem.attijari_compass.dto.report.ReportCashCategoryResponse;
import com.adem.attijari_compass.dto.report.ReportCategoryResponse;
import com.adem.attijari_compass.dto.report.ReportSummaryResponse;
import com.adem.attijari_compass.entity.BudgetTarget;
import com.adem.attijari_compass.entity.BudgetTargetStatus;
import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCashBreakdown;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.BudgetTargetRepository;
import com.adem.attijari_compass.repository.TransactionCashBreakdownRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final double EPSILON = 0.01d;

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionCashBreakdownRepository transactionCashBreakdownRepository;
    private final BudgetTargetRepository budgetTargetRepository;
    private final BudgetTargetAlertService budgetTargetAlertService;

    public ReportSummaryResponse getSummary(String email, String requestedMonth) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        YearMonth month = resolveMonth(requestedMonth);
        List<Transaction> transactions = transactionRepository.findAllByUserIdAndDateBetween(
                user.getId(),
                month.atDay(1),
                month.atEndOfMonth()
        );
        List<Transaction> expenseTransactions = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.DEPENSE)
                .toList();

        double income = sumByType(transactions, TransactionType.REVENU);
        double expenses = sumByType(transactions, TransactionType.DEPENSE);
        double netBalance = roundMoney(income - expenses);
        double savingsRate = income > 0d ? roundMoney((netBalance / income) * 100d) : 0d;
        int trackedTransactions = transactions.size();

        Map<TransactionCategory, Double> spentByCategory = aggregateSpentByCategory(expenseTransactions);
        Map<TransactionCategory, Double> budgetByCategory = loadBudgets(user.getId());
        List<ReportCategoryResponse> categoryResponses = buildCategoryResponses(spentByCategory, budgetByCategory);
        ReportCashBreakdownResponse cashBreakdown = buildCashBreakdown(expenseTransactions, expenses);
        List<BudgetAlertResponse> alerts = budgetTargetAlertService.getAlertsForCurrentUser(email);

        return ReportSummaryResponse.builder()
                .month(month.format(MONTH_FORMATTER))
                .monthLabel(capitalizeMonthLabel(month))
                .income(roundMoney(income))
                .expenses(roundMoney(expenses))
                .netBalance(netBalance)
                .savingsRate(savingsRate)
                .trackedTransactions(trackedTransactions)
                .alertCount(alerts.size())
                .categories(categoryResponses)
                .cashBreakdown(cashBreakdown)
                .build();
    }

    private Map<TransactionCategory, Double> loadBudgets(Long userId) {
        Map<TransactionCategory, Double> budgetByCategory = new EnumMap<>(TransactionCategory.class);
        List<BudgetTarget> activeBudgets = budgetTargetRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(
                userId,
                BudgetTargetStatus.ACTIVE
        );

        for (BudgetTarget budget : activeBudgets) {
            if (budget.getCategory() == null || budgetByCategory.containsKey(budget.getCategory())) {
                continue;
            }

            double amount = budget.getSuggestedMonthlyAmount() != null
                    ? budget.getSuggestedMonthlyAmount().doubleValue()
                    : 0d;
            budgetByCategory.put(budget.getCategory(), roundMoney(amount));
        }

        return budgetByCategory;
    }

    private Map<TransactionCategory, Double> aggregateSpentByCategory(List<Transaction> transactions) {
        Map<TransactionCategory, Double> totals = new EnumMap<>(TransactionCategory.class);

        for (Transaction transaction : transactions) {
            TransactionCategory category = transaction.getCategory() != null ? transaction.getCategory() : TransactionCategory.fallback();
            double amount = Math.abs(transaction.getAmount() != null ? transaction.getAmount() : 0d);
            if (amount <= 0d) {
                continue;
            }

            totals.merge(category, amount, Double::sum);
        }

        return totals;
    }

    private List<ReportCategoryResponse> buildCategoryResponses(
            Map<TransactionCategory, Double> spentByCategory,
            Map<TransactionCategory, Double> budgetByCategory
    ) {
        LinkedHashSet<TransactionCategory> categories = new LinkedHashSet<>();
        categories.addAll(spentByCategory.keySet());
        categories.addAll(budgetByCategory.keySet());

        List<ReportCategoryResponse> results = new ArrayList<>();
        for (TransactionCategory category : categories) {
            double spent = roundMoney(spentByCategory.getOrDefault(category, 0d));
            double budget = roundMoney(budgetByCategory.getOrDefault(category, 0d));
            if (spent <= 0d && budget <= 0d) {
                continue;
            }

            Double usagePercent = budget > 0d ? roundMoney((spent / budget) * 100d) : null;
            Double remaining = budget > 0d ? roundMoney(budget - spent) : null;

            results.add(ReportCategoryResponse.builder()
                    .category(category.name())
                    .categoryLabel(humanizeCategory(category))
                    .icon(resolveCategoryIcon(category))
                    .budget(budget > 0d ? budget : 0d)
                    .spent(spent)
                    .usagePercent(usagePercent)
                    .remainingAmount(remaining)
                    .status(resolveCategoryStatus(budget, spent, usagePercent))
                    .advice(buildAdvice(budget, spent, usagePercent))
                    .build());
        }

        results.sort(Comparator
                .comparing((ReportCategoryResponse item) -> item.getSpent() != null ? item.getSpent() : 0d)
                .reversed()
                .thenComparing(ReportCategoryResponse::getCategoryLabel, Comparator.nullsLast(String::compareToIgnoreCase)));
        return results;
    }

    private ReportCashBreakdownResponse buildCashBreakdown(List<Transaction> expenseTransactions, double totalExpenses) {
        List<Transaction> cashTransactions = expenseTransactions.stream()
                .filter(transaction -> transaction.getPaymentMethod() == PaymentMethod.CASH)
                .toList();

        if (cashTransactions.isEmpty()) {
            return ReportCashBreakdownResponse.builder()
                    .totalCashExpenses(0d)
                    .shareOfExpenses(0d)
                    .transactionCount(0)
                    .completedBreakdowns(0)
                    .pendingBreakdowns(0)
                    .averageTransactionAmount(0d)
                    .categories(List.of())
                    .build();
        }

        Map<Long, List<TransactionCashBreakdown>> breakdownsByTransactionId = loadBreakdowns(cashTransactions);
        Map<TransactionCategory, CashAggregation> categoryTotals = new EnumMap<>(TransactionCategory.class);
        double totalCashExpenses = 0d;
        int completedBreakdowns = 0;
        int pendingBreakdowns = 0;

        for (Transaction transaction : cashTransactions) {
            double transactionAmount = Math.abs(transaction.getAmount() != null ? transaction.getAmount() : 0d);
            totalCashExpenses += transactionAmount;

            List<TransactionCashBreakdown> items = breakdownsByTransactionId.getOrDefault(transaction.getId(), List.of());
            double allocated = 0d;

            if (!items.isEmpty()) {
                for (TransactionCashBreakdown item : items) {
                    double itemAmount = roundMoney(item.getAmount());
                    if (itemAmount <= 0d) {
                        continue;
                    }

                    allocated += itemAmount;
                    categoryTotals
                            .computeIfAbsent(item.getCategory(), ignored -> new CashAggregation())
                            .register(itemAmount);
                }
            }

            double remaining = roundMoney(transactionAmount - allocated);
            if (remaining > EPSILON || items.isEmpty()) {
                TransactionCategory fallbackCategory = transaction.getCategory() != null
                        ? transaction.getCategory()
                        : TransactionCategory.fallback();
                categoryTotals
                        .computeIfAbsent(fallbackCategory, ignored -> new CashAggregation())
                        .register(items.isEmpty() ? transactionAmount : remaining);
            }

            if (!items.isEmpty() && Math.abs(transactionAmount - allocated) <= EPSILON) {
                completedBreakdowns++;
            } else {
                pendingBreakdowns++;
            }
        }

        double safeTotalCashExpenses = roundMoney(totalCashExpenses);
        List<ReportCashCategoryResponse> categories = categoryTotals.entrySet().stream()
                .sorted((left, right) -> Double.compare(right.getValue().amount, left.getValue().amount))
                .map(entry -> ReportCashCategoryResponse.builder()
                        .category(entry.getKey().name())
                        .categoryLabel(humanizeCategory(entry.getKey()))
                        .amount(roundMoney(entry.getValue().amount))
                        .share(safeTotalCashExpenses > 0d
                                ? roundMoney((entry.getValue().amount / safeTotalCashExpenses) * 100d)
                                : 0d)
                        .transactionCount(entry.getValue().count)
                        .build())
                .toList();

        return ReportCashBreakdownResponse.builder()
                .totalCashExpenses(safeTotalCashExpenses)
                .shareOfExpenses(totalExpenses > 0d ? roundMoney((safeTotalCashExpenses / totalExpenses) * 100d) : 0d)
                .transactionCount(cashTransactions.size())
                .completedBreakdowns(completedBreakdowns)
                .pendingBreakdowns(pendingBreakdowns)
                .averageTransactionAmount(cashTransactions.isEmpty() ? 0d : roundMoney(safeTotalCashExpenses / cashTransactions.size()))
                .categories(categories)
                .build();
    }

    private Map<Long, List<TransactionCashBreakdown>> loadBreakdowns(Collection<Transaction> cashTransactions) {
        List<Long> transactionIds = cashTransactions.stream()
                .map(Transaction::getId)
                .toList();

        Map<Long, List<TransactionCashBreakdown>> itemsByTransactionId = new LinkedHashMap<>();
        for (TransactionCashBreakdown item : transactionCashBreakdownRepository.findAllByTransaction_IdIn(transactionIds)) {
            Long transactionId = item.getTransaction() != null ? item.getTransaction().getId() : null;
            if (transactionId == null) {
                continue;
            }

            itemsByTransactionId.computeIfAbsent(transactionId, ignored -> new ArrayList<>()).add(item);
        }
        return itemsByTransactionId;
    }

    private double sumByType(List<Transaction> transactions, TransactionType type) {
        return roundMoney(transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .filter(amount -> amount != null && Double.isFinite(amount))
                .mapToDouble(amount -> Math.abs(amount))
                .sum());
    }

    private YearMonth resolveMonth(String requestedMonth) {
        if (!StringUtils.hasText(requestedMonth)) {
            return YearMonth.now();
        }

        try {
            return YearMonth.parse(requestedMonth.trim(), MONTH_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("month must use format yyyy-MM, for example 2026-04");
        }
    }

    private String buildAdvice(double budget, double spent, Double usagePercent) {
        if (spent <= 0d && budget > 0d) {
            return "Aucune depense observee pour cette categorie ce mois-ci.";
        }

        if (budget <= 0d && spent > 0d) {
            return "Aucun budget defini pour cette categorie. Pensez a fixer un seuil mensuel.";
        }

        if (usagePercent == null) {
            return "Lecture partielle de la categorie.";
        }

        if (usagePercent > 100d) {
            return "Depassement constate. Reduisez les depenses ou reajustez le budget cible.";
        }
        if (usagePercent >= 90d) {
            return "Categorie critique. Le seuil mensuel est presque atteint.";
        }
        if (usagePercent >= 70d) {
            return "Categorie a surveiller. Le rythme de depense accelere.";
        }

        return "Categorie globalement sous controle pour le moment.";
    }

    private String resolveCategoryStatus(double budget, double spent, Double usagePercent) {
        if (budget <= 0d && spent > 0d) {
            return "NO_BUDGET";
        }
        if (usagePercent == null) {
            return "INFO";
        }
        if (usagePercent > 100d) {
            return "OVER_LIMIT";
        }
        if (usagePercent >= 90d) {
            return "WARNING";
        }
        if (usagePercent >= 70d) {
            return "WATCH";
        }
        return "ON_TRACK";
    }

    private String humanizeCategory(TransactionCategory category) {
        if (category == null) {
            return TransactionCategory.fallback().label();
        }
        return category.label();
    }

    private String resolveCategoryIcon(TransactionCategory category) {
        if (category == null) {
            return TransactionCategory.fallback().iconName();
        }
        return category.iconName();
    }

    private String capitalizeMonthLabel(YearMonth month) {
        String label = month.format(MONTH_LABEL_FORMATTER);
        if (label.isBlank()) {
            return month.format(MONTH_FORMATTER);
        }
        return label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1);
    }

    private double roundMoney(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private static final class CashAggregation {
        private double amount;
        private int count;

        private void register(double additionalAmount) {
            amount += additionalAmount;
            count += 1;
        }
    }
}
