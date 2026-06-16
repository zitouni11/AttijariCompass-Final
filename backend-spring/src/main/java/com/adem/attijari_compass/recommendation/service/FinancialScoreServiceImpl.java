package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.entity.BudgetMonitoringStatus;
import com.adem.attijari_compass.entity.BudgetTarget;
import com.adem.attijari_compass.entity.BudgetTargetLevel;
import com.adem.attijari_compass.entity.BudgetTargetStatus;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.FinancialScoreBreakdownDto;
import com.adem.attijari_compass.recommendation.dto.FinancialScoreFactorDto;
import com.adem.attijari_compass.recommendation.enums.CurrentMonthSeverity;
import com.adem.attijari_compass.recommendation.expense.ExpenseCategoryPolicy;
import com.adem.attijari_compass.recommendation.expense.ExpenseMetricsSnapshot;
import com.adem.attijari_compass.repository.BudgetTargetRepository;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.service.BudgetTargetMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FinancialScoreServiceImpl implements FinancialScoreService {

    private static final int BASE_SCORE = 100;
    private static final int BONUS_CAP = 12;
    private static final int DEFAULT_ANALYSIS_WINDOW_DAYS = 30;
    private static final int INCOME_STABILITY_BUCKETS = 4;
    private static final int INCOME_STABILITY_BUCKET_DAYS = 30;
    private static final int CRITICAL_NO_INCOME_SCORE_CAP = 35;
    private static final int CRITICAL_MONTH_SCORE_CAP = 49;
    private static final double SIGNIFICANT_EXPENSE_THRESHOLD = 300.0d;
    private static final BigDecimal STRONG_PRIORITY_BUDGET_THRESHOLD = new BigDecimal("120.00");
    private static final Set<TransactionCategory> PRIORITY_BUDGET_CATEGORIES = EnumSet.of(
            TransactionCategory.ALIMENTATION,
            TransactionCategory.BANQUE,
            TransactionCategory.EDUCATION,
            TransactionCategory.FACTURES,
            TransactionCategory.LOGEMENT,
            TransactionCategory.OPERATEURS_TELEPHONIQUES,
            TransactionCategory.SANTE,
            TransactionCategory.STEG_SONEDE,
            TransactionCategory.SUPERMARCHE,
            TransactionCategory.TRANSPORT
    );

    private final TransactionRepository transactionRepository;
    private final BudgetTargetRepository budgetTargetRepository;
    private final BudgetTargetMonitoringService budgetTargetMonitoringService;

    @Override
    public FinancialScoreBreakdownDto calculate(
            Long userId,
            FinancialInsightDto insight,
            ExpenseMetricsSnapshot expenseSnapshot
    ) {
        double expenseIncreaseRatio = resolveExpenseIncreaseRatio(expenseSnapshot);
        List<FinancialScoreFactorDto> penalties = new ArrayList<>();
        List<FinancialScoreFactorDto> bonuses = new ArrayList<>();
        MonthlySituationAssessment monthlySituation = assessMonthlySituation(userId, insight);

        addRecentExpenseSpikePenalty(expenseSnapshot, penalties);
        addDominantCategoryPenalty(expenseSnapshot, penalties);
        addSecondDominantCategoryPenalty(expenseSnapshot, penalties);
        addAnomalyPenalty(insight, expenseSnapshot, penalties);
        addFixedChargesPressurePenalty(expenseSnapshot, penalties);
        addSevereExpenseSpikePenalty(expenseIncreaseRatio, penalties);
        addCurrentMonthCriticalPenalties(monthlySituation, penalties);

        addHealthySavingsBonus(insight, bonuses);
        addIncomeStabilityBonus(userId, expenseSnapshot, bonuses);
        addGoalTrajectoryBonus(insight, bonuses);

        int penaltyPoints = penalties.stream()
                .map(FinancialScoreFactorDto::getPoints)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int rawBonusPoints = bonuses.stream()
                .map(FinancialScoreFactorDto::getPoints)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int bonusCap = expenseIncreaseRatio > 1.5d
                ? Math.min(BONUS_CAP, 5)
                : BONUS_CAP;
        int appliedBonusPoints = Math.min(rawBonusPoints, bonusCap);
        int rawScore = BASE_SCORE + penaltyPoints + appliedBonusPoints;
        int boundedScore = clampScore(rawScore);
        ScoreCapResult capResult = applyCurrentMonthScoreCap(boundedScore, monthlySituation);
        int finalScore = capResult.finalScore();
        String finalLabel = resolveScoreLabel(finalScore, monthlySituation);

        logAppliedFactors(userId, "penalties", penalties);
        logAppliedFactors(userId, "bonuses", bonuses);
        log.info(
                "Financial score computed: userId={}, rawScore={}, boundedScore={}, penaltyPoints={}, rawBonusPoints={}, appliedBonusPoints={}, finalScore={}, finalLabel={}, currentMonthSeverity={}, criticalMonthlySituation={}, appliedScoreCap={}",
                userId,
                rawScore,
                boundedScore,
                penaltyPoints,
                rawBonusPoints,
                appliedBonusPoints,
                finalScore,
                finalLabel,
                monthlySituation.severity(),
                monthlySituation.critical(),
                capResult.appliedCap()
        );

        return FinancialScoreBreakdownDto.builder()
                .rawScore(rawScore)
                .finalScore(finalScore)
                .baseScore(BASE_SCORE)
                .penaltyPoints(penaltyPoints)
                .rawBonusPoints(rawBonusPoints)
                .bonusPoints(appliedBonusPoints)
                .bonusCapApplied(rawBonusPoints > bonusCap)
                .appliedScoreCap(capResult.appliedCap())
                .label(finalLabel)
                .criticalMonthlySituation(monthlySituation.critical())
                .currentMonthSeverity(monthlySituation.severity())
                .penalties(penalties)
                .bonuses(bonuses)
                .build();
    }

    private MonthlySituationAssessment assessMonthlySituation(Long userId, FinancialInsightDto insight) {
        double monthlyIncome = safe(insight != null ? insight.getTotalIncome() : null);
        double monthlyExpenses = safe(insight != null ? insight.getTotalExpenses() : null);
        double netBalance = insight != null && insight.getRemainingBalance() != null
                ? safe(insight.getRemainingBalance())
                : round(monthlyIncome - monthlyExpenses);
        double savingsRate = safe(insight != null ? insight.getSavingsRate() : null);

        List<BudgetTarget> loadedBudgetTargets = userId != null
                ? budgetTargetRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, BudgetTargetStatus.ACTIVE)
                : List.of();
        List<BudgetTarget> activeBudgetTargets = loadedBudgetTargets != null ? loadedBudgetTargets : List.of();
        Map<Long, BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot> loadedBudgetSnapshots = userId != null
                ? budgetTargetMonitoringService.buildSnapshots(userId, activeBudgetTargets)
                : Map.of();
        Map<Long, BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot> budgetSnapshots = loadedBudgetSnapshots != null
                ? loadedBudgetSnapshots
                : Map.of();

        List<String> stronglyExceededPriorityBudgets = activeBudgetTargets.stream()
                .filter(budgetTarget -> isStronglyExceededPriorityBudget(
                        budgetTarget,
                        budgetSnapshots.get(budgetTarget.getId())
                ))
                .map(budgetTarget -> categoryLabel(budgetTarget.getCategory()))
                .distinct()
                .toList();

        boolean noIncomeWithExpenses = monthlyIncome <= 0.0d && monthlyExpenses > 0.0d;
        boolean negativeNetBalance = netBalance < 0.0d;
        boolean zeroSavingsWithSignificantExpenses = savingsRate <= 0.0d && monthlyExpenses >= SIGNIFICANT_EXPENSE_THRESHOLD;
        boolean priorityBudgetStronglyExceeded = !stronglyExceededPriorityBudgets.isEmpty();
        boolean critical = isCriticalMonthlySituation(
                noIncomeWithExpenses,
                negativeNetBalance,
                zeroSavingsWithSignificantExpenses,
                priorityBudgetStronglyExceeded
        );

        log.info(
                "Current month situation assessed: userId={}, monthlyIncome={}, monthlyExpenses={}, netBalance={}, savingsRate={}, noIncomeWithExpenses={}, negativeNetBalance={}, zeroSavingsWithSignificantExpenses={}, priorityBudgetStronglyExceeded={}, exceededPriorityBudgets={}, critical={}",
                userId,
                formatMoney(monthlyIncome),
                formatMoney(monthlyExpenses),
                formatMoney(netBalance),
                formatPercent(savingsRate / 100.0d),
                noIncomeWithExpenses,
                negativeNetBalance,
                zeroSavingsWithSignificantExpenses,
                priorityBudgetStronglyExceeded,
                stronglyExceededPriorityBudgets,
                critical
        );

        return new MonthlySituationAssessment(
                monthlyIncome,
                monthlyExpenses,
                netBalance,
                savingsRate,
                noIncomeWithExpenses,
                negativeNetBalance,
                zeroSavingsWithSignificantExpenses,
                priorityBudgetStronglyExceeded,
                stronglyExceededPriorityBudgets,
                critical ? CurrentMonthSeverity.CRITICAL : CurrentMonthSeverity.NORMAL,
                critical
        );
    }

    private boolean isCriticalMonthlySituation(
            boolean noIncomeWithExpenses,
            boolean negativeNetBalance,
            boolean zeroSavingsWithSignificantExpenses,
            boolean priorityBudgetStronglyExceeded
    ) {
        return noIncomeWithExpenses
                || negativeNetBalance
                || zeroSavingsWithSignificantExpenses
                || priorityBudgetStronglyExceeded;
    }

    private void addCurrentMonthCriticalPenalties(
            MonthlySituationAssessment monthlySituation,
            List<FinancialScoreFactorDto> penalties
    ) {
        if (monthlySituation.noIncomeWithExpenses()) {
            penalties.add(FinancialScoreFactorDto.builder()
                    .code("NO_INCOME_WITH_EXPENSES")
                    .label("Aucun revenu observe ce mois")
                    .points(-40)
                    .explanation(String.format(
                            "Le mois courant affiche %s de revenus pour %s de depenses.",
                            formatMoney(monthlySituation.monthlyIncome()),
                            formatMoney(monthlySituation.monthlyExpenses())
                    ))
                    .build());
        }

        if (monthlySituation.negativeNetBalance()) {
            penalties.add(FinancialScoreFactorDto.builder()
                    .code("NEGATIVE_NET_BALANCE")
                    .label("Solde mensuel negatif")
                    .points(-25)
                    .explanation(String.format(
                            "Le solde net du mois courant est negatif (%s).",
                            formatMoney(monthlySituation.netBalance())
                    ))
                    .build());
        }

        if (monthlySituation.priorityBudgetStronglyExceeded()) {
            String affectedBudgets = String.join(", ", monthlySituation.stronglyExceededPriorityBudgets());
            penalties.add(FinancialScoreFactorDto.builder()
                    .code("PRIORITY_BUDGET_STRONGLY_EXCEEDED")
                    .label("Budget prioritaire fortement depasse")
                    .points(-18)
                    .explanation(String.format(
                            "Un ou plusieurs budgets prioritaires sont fortement depasses ce mois-ci: %s.",
                            affectedBudgets
                    ))
                    .build());
        }

        if (monthlySituation.zeroSavingsWithSignificantExpenses()) {
            penalties.add(FinancialScoreFactorDto.builder()
                    .code("ZERO_SAVINGS_WITH_SIGNIFICANT_EXPENSES")
                    .label("Absence d'epargne mensuelle")
                    .points(-15)
                    .explanation(String.format(
                            "Le taux d'epargne est nul alors que les depenses du mois atteignent %s.",
                            formatMoney(monthlySituation.monthlyExpenses())
                    ))
                    .build());
        }
    }

    private ScoreCapResult applyCurrentMonthScoreCap(
            int score,
            MonthlySituationAssessment monthlySituation
    ) {
        if (monthlySituation.noIncomeWithExpenses()) {
            return new ScoreCapResult(Math.min(score, CRITICAL_NO_INCOME_SCORE_CAP), CRITICAL_NO_INCOME_SCORE_CAP);
        }

        if (monthlySituation.critical()) {
            return new ScoreCapResult(Math.min(score, CRITICAL_MONTH_SCORE_CAP), CRITICAL_MONTH_SCORE_CAP);
        }

        return new ScoreCapResult(score, null);
    }

    private boolean isStronglyExceededPriorityBudget(
            BudgetTarget budgetTarget,
            BudgetTargetMonitoringService.BudgetTargetMonitoringSnapshot snapshot
    ) {
        if (budgetTarget == null || snapshot == null || snapshot.usagePercent() == null) {
            return false;
        }

        boolean exceeded = snapshot.monitoringStatus() == BudgetMonitoringStatus.DEPASSE
                && snapshot.usagePercent().compareTo(STRONG_PRIORITY_BUDGET_THRESHOLD) >= 0;
        boolean priorityBudget = budgetTarget.getSelectedLevel() == BudgetTargetLevel.RENFORCE
                || PRIORITY_BUDGET_CATEGORIES.contains(budgetTarget.getCategory());

        return exceeded && priorityBudget;
    }

    private void addSevereExpenseSpikePenalty(
            double expenseIncreaseRatio,
            List<FinancialScoreFactorDto> penalties
    ) {
        if (expenseIncreaseRatio <= 3.0d) {
            return;
        }

        penalties.add(FinancialScoreFactorDto.builder()
                .code("SEVERE_EXPENSE_SPIKE")
                .label("Explosion recente des depenses")
                .points(-40)
                .explanation(String.format(
                        "Les depenses recentes depassent de %s la moyenne historique, ce qui signale une derive tres forte.",
                        formatPercent(expenseIncreaseRatio)
                ))
                .build());
    }

    private void addRecentExpenseSpikePenalty(
            ExpenseMetricsSnapshot snapshot,
            List<FinancialScoreFactorDto> penalties
    ) {
        double baselineMonthlyAverage = safeMoney(snapshot != null ? snapshot.getBaselineMonthlyAverage() : null);
        double recentMonthlyEquivalent = normalizedRecentExpense(snapshot);
        if (baselineMonthlyAverage <= 0.0d || recentMonthlyEquivalent <= baselineMonthlyAverage) {
            return;
        }

        double increaseRatio = (recentMonthlyEquivalent - baselineMonthlyAverage) / baselineMonthlyAverage;
        int points;
        if (increaseRatio > 0.70d) {
            points = -30;
        } else if (increaseRatio > 0.40d) {
            points = -22;
        } else if (increaseRatio > 0.20d) {
            points = -15;
        } else if (increaseRatio > 0.10d) {
            points = -8;
        } else {
            return;
        }

        penalties.add(FinancialScoreFactorDto.builder()
                .code("RECENT_EXPENSE_SPIKE")
                .label("Hausse globale des depenses recentes")
                .points(points)
                .explanation(String.format(
                        "Depenses recentes equivalentes sur 30 jours: %s contre une moyenne historique de %s (+%s).",
                        formatMoney(recentMonthlyEquivalent),
                        formatMoney(baselineMonthlyAverage),
                        formatPercent(increaseRatio)
                ))
                .build());
    }

    private void addDominantCategoryPenalty(
            ExpenseMetricsSnapshot snapshot,
            List<FinancialScoreFactorDto> penalties
    ) {
        List<CategoryShare> topCategories = resolveCategoryShares(snapshot);
        if (topCategories.isEmpty()) {
            return;
        }

        CategoryShare dominantCategory = topCategories.get(0);
        int points;
        if (dominantCategory.share() > 0.50d) {
            points = -14;
        } else if (dominantCategory.share() > 0.40d) {
            points = -10;
        } else if (dominantCategory.share() > 0.30d) {
            points = -6;
        } else {
            return;
        }

        penalties.add(FinancialScoreFactorDto.builder()
                .code("DOMINANT_CATEGORY")
                .label("Categorie dominante trop concentree")
                .points(points)
                .explanation(String.format(
                        "La categorie %s represente %s des depenses analysees.",
                        categoryLabel(dominantCategory.category()),
                        formatPercent(dominantCategory.share())
                ))
                .build());
    }

    private void addSecondDominantCategoryPenalty(
            ExpenseMetricsSnapshot snapshot,
            List<FinancialScoreFactorDto> penalties
    ) {
        List<CategoryShare> topCategories = resolveCategoryShares(snapshot);
        if (topCategories.size() < 2) {
            return;
        }

        CategoryShare secondCategory = topCategories.get(1);
        if (secondCategory.share() <= 0.30d) {
            return;
        }

        penalties.add(FinancialScoreFactorDto.builder()
                .code("SECOND_DOMINANT_CATEGORY")
                .label("Deuxieme categorie dominante elevee")
                .points(-5)
                .explanation(String.format(
                        "La deuxieme categorie, %s, represente encore %s des depenses analysees.",
                        categoryLabel(secondCategory.category()),
                        formatPercent(secondCategory.share())
                ))
                .build());
    }

    private void addAnomalyPenalty(
            FinancialInsightDto insight,
            ExpenseMetricsSnapshot snapshot,
            List<FinancialScoreFactorDto> penalties
    ) {
        if (!Boolean.TRUE.equals(insight != null ? insight.getAnomalyDetected() : null)) {
            return;
        }

        double anomalyAmount = safe(insight != null ? insight.getAnomalyAmount() : null);
        double recentMonthlyEquivalent = normalizedRecentExpense(snapshot);
        double strongThreshold = Math.max(500.0d, recentMonthlyEquivalent * 0.25d);
        double mediumThreshold = Math.max(250.0d, recentMonthlyEquivalent * 0.15d);

        int points;
        String severityLabel;
        if (anomalyAmount >= strongThreshold) {
            points = -15;
            severityLabel = "forte";
        } else if (anomalyAmount >= mediumThreshold) {
            points = -10;
            severityLabel = "moyenne";
        } else {
            points = -5;
            severityLabel = "legere";
        }

        penalties.add(FinancialScoreFactorDto.builder()
                .code("EXPENSE_ANOMALY")
                .label("Anomalie de depense detectee")
                .points(points)
                .explanation(String.format(
                        "Anomalie %s detectee avec un montant de %s.",
                        severityLabel,
                        formatMoney(anomalyAmount)
                ))
                .build());
    }

    private void addFixedChargesPressurePenalty(
            ExpenseMetricsSnapshot snapshot,
            List<FinancialScoreFactorDto> penalties
    ) {
        double fixedChargesTotal = safeMoney(snapshot != null ? snapshot.getFixedChargesTotal() : null);
        double incomeTotal = safeMoney(snapshot != null ? snapshot.getAnalysisIncomeTotal() : null);

        if (fixedChargesTotal <= 0.0d) {
            return;
        }

        if (incomeTotal <= 0.0d) {
            penalties.add(FinancialScoreFactorDto.builder()
                    .code("FIXED_CHARGES_PRESSURE")
                    .label("Pression des charges fixes")
                    .points(-18)
                    .explanation("Des charges fixes sont presentes alors qu'aucun revenu recent n'a ete observe sur la periode analysee.")
                    .build());
            return;
        }

        double ratio = fixedChargesTotal / incomeTotal;
        int points;
        if (ratio > 0.70d) {
            points = -18;
        } else if (ratio > 0.55d) {
            points = -12;
        } else if (ratio > 0.40d) {
            points = -6;
        } else {
            return;
        }

        penalties.add(FinancialScoreFactorDto.builder()
                .code("FIXED_CHARGES_PRESSURE")
                .label("Pression des charges fixes")
                .points(points)
                .explanation(String.format(
                        "Les charges fixes representent %s des revenus observes sur la periode analysee.",
                        formatPercent(ratio)
                ))
                .build());
    }

    private void addHealthySavingsBonus(
            FinancialInsightDto insight,
            List<FinancialScoreFactorDto> bonuses
    ) {
        double savingsRate = safe(insight != null ? insight.getSavingsRate() : null);
        int points;
        if (savingsRate >= 25.0d) {
            points = 8;
        } else if (savingsRate >= 20.0d) {
            points = 6;
        } else if (savingsRate >= 15.0d) {
            points = 4;
        } else {
            return;
        }

        bonuses.add(FinancialScoreFactorDto.builder()
                .code("HEALTHY_SAVINGS")
                .label("Epargne saine")
                .points(points)
                .explanation(String.format(
                        "Le taux d'epargne atteint %s.",
                        formatPercent(savingsRate / 100.0d)
                ))
                .build());
    }

    private void addIncomeStabilityBonus(
            Long userId,
            ExpenseMetricsSnapshot snapshot,
            List<FinancialScoreFactorDto> bonuses
    ) {
        IncomeStabilityMetrics metrics = calculateIncomeStability(userId, snapshot);
        if (metrics.activeBuckets() < 3L || metrics.averageIncome() <= 0.0d) {
            return;
        }

        int points;
        if (metrics.coefficientOfVariation() <= 0.10d) {
            points = 5;
        } else if (metrics.coefficientOfVariation() <= 0.20d) {
            points = 2;
        } else {
            return;
        }

        bonuses.add(FinancialScoreFactorDto.builder()
                .code("INCOME_STABILITY")
                .label("Stabilite des revenus")
                .points(points)
                .explanation(String.format(
                        "Les revenus restent reguliers sur %d periodes recentes de 30 jours (variation %s).",
                        metrics.activeBuckets(),
                        formatPercent(metrics.coefficientOfVariation())
                ))
                .build());
    }

    private void addGoalTrajectoryBonus(
            FinancialInsightDto insight,
            List<FinancialScoreFactorDto> bonuses
    ) {
        double requiredContribution = safe(insight != null ? insight.getRequiredMonthlyContributionForGoal() : null);
        double currentContribution = safe(insight != null ? insight.getCurrentMonthlyContribution() : null);
        if (requiredContribution <= 0.0d || currentContribution <= 0.0d) {
            return;
        }

        double coverageRatio = currentContribution / requiredContribution;
        int points;
        if (coverageRatio >= 1.0d) {
            points = 5;
        } else if (coverageRatio >= 0.85d) {
            points = 3;
        } else {
            return;
        }

        bonuses.add(FinancialScoreFactorDto.builder()
                .code("GOAL_TRAJECTORY")
                .label("Trajectoire d'objectif positive")
                .points(points)
                .explanation(String.format(
                        "La contribution actuelle couvre %s du besoin mensuel pour l'objectif.",
                        formatPercent(coverageRatio)
                ))
                .build());
    }

    private List<CategoryShare> resolveCategoryShares(ExpenseMetricsSnapshot snapshot) {
        BigDecimal expenseTotal = snapshot != null ? snapshot.getAnalysisExpenseTotal() : BigDecimal.ZERO;
        if (expenseTotal == null || expenseTotal.signum() <= 0) {
            return List.of();
        }

        Map<TransactionCategory, BigDecimal> categoryTotals = snapshot != null ? snapshot.getAnalysisCategoryTotals() : null;
        if (categoryTotals == null || categoryTotals.isEmpty()) {
            return List.of();
        }

        List<CategoryShare> shares = new ArrayList<>();
        for (Map.Entry<TransactionCategory, BigDecimal> entry : categoryTotals.entrySet()) {
            if (!ExpenseCategoryPolicy.isExpenseEligible(entry.getKey())) {
                continue;
            }

            BigDecimal categoryTotal = safeMoneyValue(entry.getValue());
            if (categoryTotal.signum() <= 0) {
                continue;
            }

            double share = categoryTotal
                    .divide(expenseTotal, 6, RoundingMode.HALF_UP)
                    .doubleValue();
            shares.add(new CategoryShare(entry.getKey(), share));
        }

        shares.sort(Comparator.comparing(CategoryShare::share).reversed());
        return shares;
    }

    private IncomeStabilityMetrics calculateIncomeStability(Long userId, ExpenseMetricsSnapshot snapshot) {
        if (userId == null) {
            return IncomeStabilityMetrics.empty();
        }

        LocalDate analysisEndDate = snapshot != null && snapshot.getAnalysisEndDate() != null
                ? snapshot.getAnalysisEndDate()
                : LocalDate.now();
        LocalDate historyStartDate = analysisEndDate.minusDays((long) INCOME_STABILITY_BUCKETS * INCOME_STABILITY_BUCKET_DAYS - 1L);
        List<Transaction> transactions = transactionRepository.findAllByUserIdAndDateBetween(
                userId,
                historyStartDate,
                analysisEndDate
        );

        double[] bucketTotals = new double[INCOME_STABILITY_BUCKETS];
        for (Transaction transaction : transactions) {
            if (transaction == null || transaction.getDate() == null || transaction.getType() != TransactionType.REVENU) {
                continue;
            }

            long daysFromEnd = ChronoUnit.DAYS.between(transaction.getDate(), analysisEndDate);
            if (daysFromEnd < 0L || daysFromEnd >= (long) INCOME_STABILITY_BUCKETS * INCOME_STABILITY_BUCKET_DAYS) {
                continue;
            }

            int bucketIndex = (int) (daysFromEnd / INCOME_STABILITY_BUCKET_DAYS);
            bucketTotals[bucketIndex] += Math.abs(safe(transaction.getAmount()));
        }

        double averageIncome = average(bucketTotals);
        long activeBuckets = 0L;
        for (double bucketTotal : bucketTotals) {
            if (bucketTotal > 0.0d) {
                activeBuckets++;
            }
        }

        if (averageIncome <= 0.0d) {
            return new IncomeStabilityMetrics(0L, 0.0d, Double.POSITIVE_INFINITY);
        }

        double variance = 0.0d;
        for (double bucketTotal : bucketTotals) {
            variance += Math.pow(bucketTotal - averageIncome, 2);
        }
        variance = variance / bucketTotals.length;
        double standardDeviation = Math.sqrt(variance);

        return new IncomeStabilityMetrics(activeBuckets, averageIncome, standardDeviation / averageIncome);
    }

    private double normalizedRecentExpense(ExpenseMetricsSnapshot snapshot) {
        double expenseTotal = safeMoney(snapshot != null ? snapshot.getAnalysisExpenseTotal() : null);
        int windowDays = snapshot != null ? snapshot.getAnalysisWindowDays() : DEFAULT_ANALYSIS_WINDOW_DAYS;
        int safeWindowDays = windowDays > 0 ? windowDays : DEFAULT_ANALYSIS_WINDOW_DAYS;
        return round((expenseTotal / safeWindowDays) * DEFAULT_ANALYSIS_WINDOW_DAYS);
    }

    private double resolveExpenseIncreaseRatio(ExpenseMetricsSnapshot snapshot) {
        double baselineMonthlyAverage = safeMoney(snapshot != null ? snapshot.getBaselineMonthlyAverage() : null);
        double recentMonthlyEquivalent = normalizedRecentExpense(snapshot);
        if (baselineMonthlyAverage <= 0.0d) {
            return 0.0d;
        }
        return (recentMonthlyEquivalent - baselineMonthlyAverage) / baselineMonthlyAverage;
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String resolveScoreLabel(int score, MonthlySituationAssessment monthlySituation) {
        if (monthlySituation.critical() && score >= 50) {
            return "A surveiller";
        }
        if (score >= 80) {
            return "Excellent";
        }
        if (score >= 65) {
            return "Solide";
        }
        if (score >= 50) {
            return "A consolider";
        }
        if (score >= 35) {
            return "A surveiller";
        }
        return "Critique";
    }

    private void logAppliedFactors(Long userId, String factorType, List<FinancialScoreFactorDto> factors) {
        List<String> renderedFactors = factors.stream()
                .map(factor -> factor.getCode() + ":" + factor.getPoints())
                .collect(Collectors.toList());
        log.info("Financial score {} applied: userId={}, factors={}", factorType, userId, renderedFactors);
    }

    private String categoryLabel(TransactionCategory category) {
        if (category == null) {
            return "autres depenses";
        }
        return category == TransactionCategory.AUTRES ? "autres depenses" : category.lowerLabel();
    }

    private double average(double[] values) {
        if (values.length == 0) {
            return 0.0d;
        }

        double total = 0.0d;
        for (double value : values) {
            total += value;
        }
        return total / values.length;
    }

    private double safe(Double value) {
        return value != null ? value : 0.0d;
    }

    private double safeMoney(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0d;
    }

    private BigDecimal safeMoneyValue(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String formatMoney(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String formatPercent(double ratio) {
        return BigDecimal.valueOf(ratio * 100.0d)
                .setScale(1, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record CategoryShare(
            TransactionCategory category,
            double share
    ) {
    }

    private record IncomeStabilityMetrics(
            long activeBuckets,
            double averageIncome,
            double coefficientOfVariation
    ) {
        private static IncomeStabilityMetrics empty() {
            return new IncomeStabilityMetrics(0L, 0.0d, Double.POSITIVE_INFINITY);
        }
    }

    private record MonthlySituationAssessment(
            double monthlyIncome,
            double monthlyExpenses,
            double netBalance,
            double savingsRate,
            boolean noIncomeWithExpenses,
            boolean negativeNetBalance,
            boolean zeroSavingsWithSignificantExpenses,
            boolean priorityBudgetStronglyExceeded,
            List<String> stronglyExceededPriorityBudgets,
            CurrentMonthSeverity severity,
            boolean critical
    ) {
    }

    private record ScoreCapResult(
            int finalScore,
            Integer appliedCap
    ) {
    }
}
