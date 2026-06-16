package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.dashboard.DashboardExpenseCategoryResponse;
import com.adem.attijari_compass.dto.dashboard.DashboardFinancialHealthResponse;
import com.adem.attijari_compass.dto.dashboard.DashboardResponse;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal SIXTY_PERCENT = new BigDecimal("0.60");
    private static final BigDecimal EIGHTY_PERCENT = new BigDecimal("0.80");
    private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal TWENTY_PERCENT = new BigDecimal("20");
    private static final BigDecimal TEN_PERCENT_POINTS = new BigDecimal("10");

    private final UserRepository userRepository;
    private final DashboardTransactionQueryService dashboardTransactionQueryService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String email) {
        return getDashboard(email, null);
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String email, String requestedMonth) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        YearMonth month = resolveMonth(requestedMonth);
        List<DashboardTransactionQueryService.DashboardTransactionSnapshot> transactions =
                dashboardTransactionQueryService.loadMonthlyTransactions(user.getId(), month);

        BigDecimal income = sumIncome(transactions);
        BigDecimal expenses = sumExpenses(transactions);
        BigDecimal netBalance = roundMoney(income.subtract(expenses));
        BigDecimal savingsRate = computeSavingsRate(income, netBalance);
        int trackedTransactions = transactions.size();

        List<DashboardExpenseCategoryResponse> expenseByCategory = buildExpenseByCategory(transactions);
        Map<String, BigDecimal> legacyExpenseMap = buildLegacyExpenseMap(expenseByCategory);
        DashboardFinancialHealthResponse financialHealth = buildFinancialHealth(
                income,
                expenses,
                netBalance,
                savingsRate,
                trackedTransactions
        );

        return DashboardResponse.builder()
                .month(month.format(MONTH_FORMATTER))
                .income(income)
                .expenses(expenses)
                .netBalance(netBalance)
                .savingsRate(savingsRate)
                .trackedTransactions(trackedTransactions)
                .expenseByCategory(expenseByCategory)
                .financialHealth(financialHealth)
                .totalRevenu(income)
                .totalDepenses(expenses)
                .soldeActuel(netBalance)
                .tauxEpargne(savingsRate)
                .resteAVivre(netBalance)
                .depensesParCategorie(legacyExpenseMap)
                .nombreTransactions(trackedTransactions)
                .moisCourant(capitalizeMonthLabel(month))
                .build();
    }

    private BigDecimal sumIncome(List<DashboardTransactionQueryService.DashboardTransactionSnapshot> transactions) {
        BigDecimal income = transactions.stream()
                .filter(DashboardTransactionQueryService.DashboardTransactionSnapshot::isIncome)
                .map(DashboardTransactionQueryService.DashboardTransactionSnapshot::absoluteAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return roundMoney(income);
    }

    private BigDecimal sumExpenses(List<DashboardTransactionQueryService.DashboardTransactionSnapshot> transactions) {
        BigDecimal expenses = transactions.stream()
                .filter(DashboardTransactionQueryService.DashboardTransactionSnapshot::isExpense)
                .map(DashboardTransactionQueryService.DashboardTransactionSnapshot::absoluteAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return roundMoney(expenses);
    }

    private List<DashboardExpenseCategoryResponse> buildExpenseByCategory(
            List<DashboardTransactionQueryService.DashboardTransactionSnapshot> transactions
    ) {
        Map<String, BigDecimal> totalsByCategory = new LinkedHashMap<>();

        for (DashboardTransactionQueryService.DashboardTransactionSnapshot transaction : transactions) {
            if (!transaction.isExpense()) {
                continue;
            }

            String category = normalizeExpenseCategory(transaction.category());
            if (isIncomeOnlyCategory(category)) {
                continue;
            }

            totalsByCategory.merge(category, transaction.absoluteAmount(), BigDecimal::add);
        }

        return totalsByCategory.entrySet().stream()
                .sorted((left, right) -> right.getValue().compareTo(left.getValue()))
                .map(entry -> DashboardExpenseCategoryResponse.builder()
                        .category(entry.getKey())
                        .amount(roundMoney(entry.getValue()))
                        .build())
                .toList();
    }

    private Map<String, BigDecimal> buildLegacyExpenseMap(List<DashboardExpenseCategoryResponse> expenseByCategory) {
        Map<String, BigDecimal> expenseMap = new LinkedHashMap<>();
        for (DashboardExpenseCategoryResponse entry : expenseByCategory) {
            expenseMap.put(entry.getCategory(), entry.getAmount());
        }
        return expenseMap;
    }

    private DashboardFinancialHealthResponse buildFinancialHealth(
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal netBalance,
            BigDecimal savingsRate,
            int trackedTransactions
    ) {
        LinkedHashSet<String> insights = new LinkedHashSet<>();

        if (trackedTransactions == 0) {
            insights.add("Aucune transaction reelle n'est disponible pour ce mois.");
            return DashboardFinancialHealthResponse.builder()
                    .score(0)
                    .positiveBalance(false)
                    .insights(new ArrayList<>(insights))
                    .build();
        }

        int balanceScore = computeBalanceScore(income, netBalance, insights);
        int savingsScore = computeSavingsScore(income, expenses, savingsRate, insights);
        int activityScore = computeActivityScore(trackedTransactions, insights);
        int budgetScore = computeBudgetScore(income, expenses, insights);

        int score = clamp(balanceScore + savingsScore + activityScore + budgetScore, 0, 100);

        return DashboardFinancialHealthResponse.builder()
                .score(score)
                .positiveBalance(netBalance.signum() >= 0)
                .insights(new ArrayList<>(insights))
                .build();
    }

    private int computeBalanceScore(BigDecimal income, BigDecimal netBalance, LinkedHashSet<String> insights) {
        if (netBalance.signum() >= 0) {
            insights.add("Solde net positif sur la periode.");
            return 25;
        }

        insights.add("Solde net negatif : les depenses depassent les revenus du mois.");
        if (income.signum() > 0 && netBalance.abs().compareTo(income.multiply(TEN_PERCENT)) <= 0) {
            insights.add("Le deficit reste contenu a moins de 10% des revenus.");
            return 10;
        }
        return 0;
    }

    private int computeSavingsScore(
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal savingsRate,
            LinkedHashSet<String> insights
    ) {
        if (income.signum() <= 0) {
            if (expenses.signum() == 0) {
                insights.add("Aucun revenu ni depense detecte : score de stabilite neutre.");
                return 10;
            }
            insights.add("Aucun revenu detecte sur la periode.");
            return 0;
        }

        if (savingsRate.compareTo(TWENTY_PERCENT) >= 0) {
            insights.add("Taux d'epargne solide, au moins 20% des revenus sont preserves.");
            return 30;
        }
        if (savingsRate.compareTo(TEN_PERCENT_POINTS) >= 0) {
            insights.add("Taux d'epargne correct, mais encore perfectible.");
            return 20;
        }
        if (savingsRate.signum() >= 0) {
            insights.add("Marge d'epargne faible : les depenses absorbent presque tous les revenus.");
            return 10;
        }

        insights.add("Aucune epargne ce mois-ci : les depenses depassent les revenus.");
        return 0;
    }

    private int computeActivityScore(int trackedTransactions, LinkedHashSet<String> insights) {
        if (trackedTransactions < 5) {
            insights.add("Peu de transactions suivies : le diagnostic reste partiel.");
        } else if (trackedTransactions >= 15) {
            insights.add("Volume de transactions suffisant pour une lecture fiable du mois.");
        }
        return Math.min(trackedTransactions, 20);
    }

    private int computeBudgetScore(BigDecimal income, BigDecimal expenses, LinkedHashSet<String> insights) {
        if (income.signum() <= 0) {
            return expenses.signum() == 0 ? 10 : 0;
        }

        BigDecimal expenseRatio = expenses.divide(income, 4, RoundingMode.HALF_UP);
        if (expenseRatio.compareTo(SIXTY_PERCENT) <= 0) {
            insights.add("Budget tres coherent : moins de 60% des revenus sont consommes.");
            return 25;
        }
        if (expenseRatio.compareTo(EIGHTY_PERCENT) <= 0) {
            insights.add("Budget coherent, avec une marge de manoeuvre encore confortable.");
            return 18;
        }
        if (expenseRatio.compareTo(BigDecimal.ONE) <= 0) {
            insights.add("Budget sous controle, mais la marge de securite se reduit.");
            return 10;
        }
        if (expenseRatio.compareTo(new BigDecimal("1.20")) <= 0) {
            insights.add("Budget tendu : les depenses depassent legerement les revenus.");
            return 5;
        }

        insights.add("Budget desequilibre : les depenses depassent nettement les revenus.");
        return 0;
    }

    private BigDecimal computeSavingsRate(BigDecimal income, BigDecimal netBalance) {
        if (income.signum() <= 0) {
            return ZERO;
        }
        BigDecimal savingsRate = netBalance.multiply(ONE_HUNDRED).divide(income, 4, RoundingMode.HALF_UP);
        return roundPercentage(savingsRate);
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

    private String normalizeExpenseCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return TransactionCategory.fallback().name();
        }
        return TransactionCategory.fromValue(category).name().replace('_', ' ');
    }

    private boolean isIncomeOnlyCategory(String category) {
        String normalized = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("SALAIRE")
                || normalized.equals("REVENU")
                || normalized.equals("REVENUS")
                || normalized.equals("VIREMENT")
                || normalized.equals("VERSEMENT")
                || normalized.equals("CASHBACK")
                || normalized.equals("REMBOURSEMENT");
    }

    private String capitalizeMonthLabel(YearMonth month) {
        String label = month.format(MONTH_LABEL_FORMATTER);
        if (label.isBlank()) {
            return month.format(MONTH_FORMATTER);
        }
        return label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1);
    }

    private BigDecimal roundMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal roundPercentage(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
