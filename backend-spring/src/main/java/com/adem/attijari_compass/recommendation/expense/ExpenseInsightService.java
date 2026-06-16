package com.adem.attijari_compass.recommendation.expense;

import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseInsightService {

    private static final int DEFAULT_ANALYSIS_WINDOW_DAYS = 30;
    private static final String CATEGORY_SPIKE = "CATEGORY_SPIKE";
    private static final String CATEGORY_DOMINANCE = "CATEGORY_DOMINANCE";
    private static final String MONTHLY_TOTAL_SPIKE = "MONTHLY_TOTAL_SPIKE";
    private static final String FIXED_CHARGES_PRESSURE = "FIXED_CHARGES_PRESSURE";
    private static final int RATIO_SCALE = 4;
    private static final BigDecimal DOMINANCE_IMPACT_RATE = BigDecimal.valueOf(0.10d);
    private static final BigDecimal MIN_SIGNIFICANT_DOMINANCE_IMPACT = BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal MIN_SIGNIFICANT_CATEGORY_AMOUNT = BigDecimal.valueOf(80.0d).setScale(2, RoundingMode.HALF_UP);

    private final ExpenseMetricsCalculator expenseMetricsCalculator;

    public List<ExpenseInsight> generateInsights(Long userId) {
        return generateInsights(expenseMetricsCalculator.calculate(userId));
    }

    public List<ExpenseInsight> generateInsights(ExpenseMetricsSnapshot snapshot) {
        if (snapshot == null) {
            return List.of();
        }

        List<ExpenseInsight> insights = new ArrayList<>();
        insights.addAll(detectCategorySpikes(snapshot));
        insights.addAll(detectCategoryDominance(snapshot));
        insights.addAll(detectMonthlyTotalSpike(snapshot));
        insights.addAll(detectFixedChargesPressure(snapshot));
        insights.sort(buildInsightComparator());
        return insights;
    }

    private List<ExpenseInsight> detectCategorySpikes(ExpenseMetricsSnapshot snapshot) {
        List<ExpenseInsight> insights = new ArrayList<>();
        Map<TransactionCategory, BigDecimal> currentTotals = safeMoneyMap(snapshot.getAnalysisCategoryTotals());
        Map<TransactionCategory, BigDecimal> baselineAverages = safeMoneyMap(snapshot.getBaselineCategoryAverages());
        Map<TransactionCategory, Long> counts = safeCountMap(snapshot.getAnalysisCategoryCounts());

        for (Map.Entry<TransactionCategory, BigDecimal> entry : currentTotals.entrySet()) {
            TransactionCategory category = entry.getKey();
            if (!ExpenseCategoryPolicy.isExpenseEligible(category)) {
                continue;
            }

            BigDecimal currentTotal = safeMoney(entry.getValue());
            BigDecimal baselineAverage = safeMoney(baselineAverages.get(category));
            if (baselineAverage.signum() <= 0) {
                continue;
            }

            BigDecimal triggerThreshold = baselineAverage.multiply(
                    BigDecimal.valueOf(ExpenseRecommendationThresholds.CATEGORY_SPIKE_RATIO)
            );
            if (currentTotal.compareTo(triggerThreshold) <= 0) {
                continue;
            }

            double spikeRatio = divide(currentTotal, baselineAverage);
            ExpenseCategoryProfile profile = ExpenseCategoryPolicy.resolveProfile(category);
            RecommendationPriority priority = spikeRatio >= 1.35d
                    ? RecommendationPriority.HIGH
                    : RecommendationPriority.MEDIUM;
            BigDecimal estimatedGain = positiveDifference(currentTotal, baselineAverage);

            insights.add(ExpenseInsight.builder()
                    .insightType(CATEGORY_SPIKE)
                    .category(category)
                    .profile(profile)
                    .title(buildCategorySpikeTitle(category, profile))
                    .message(buildCategorySpikeMessage(category, profile))
                    .suggestedAction(buildCategorySpikeSuggestedAction(profile))
                    .priority(priority)
                    .severityScore(computeSpikeSeverity(spikeRatio))
                    .confidenceScore(computeCategoryInsightConfidence(counts.getOrDefault(category, 0L), profile))
                    .estimatedMonthlyGain(toNullableDouble(estimatedGain))
                    .targetedTransactionsTotal(toNullableDouble(currentTotal))
                    .explanation(String.format(
                            "Le niveau %s %s est de %s contre une moyenne recente de %s.",
                            categoryLabel(category),
                            analysisPeriodQualifier(snapshot),
                            formatMoney(currentTotal),
                            formatMoney(baselineAverage)
                    ))
                    .basedOn(List.of(
                            "Seuil de declenchement: " + formatMoney(triggerThreshold)
                                    + " (" + formatPercent(ExpenseRecommendationThresholds.CATEGORY_SPIKE_RATIO) + " de la moyenne recente)",
                            observedTransactionsLabel(snapshot) + counts.getOrDefault(category, 0L),
                            analyzedPeriodLabel(snapshot)
                    ))
                    .build());
        }

        return insights;
    }

    private List<ExpenseInsight> detectCategoryDominance(ExpenseMetricsSnapshot snapshot) {
        List<ExpenseInsight> insights = new ArrayList<>();
        BigDecimal analysisExpenseTotal = safeMoney(snapshot.getAnalysisExpenseTotal());
        if (analysisExpenseTotal.signum() <= 0) {
            return insights;
        }

        Map<TransactionCategory, BigDecimal> currentTotals = safeMoneyMap(snapshot.getAnalysisCategoryTotals());
        Map<TransactionCategory, Long> counts = safeCountMap(snapshot.getAnalysisCategoryCounts());

        for (Map.Entry<TransactionCategory, BigDecimal> entry : currentTotals.entrySet()) {
            TransactionCategory category = entry.getKey();
            if (!ExpenseCategoryPolicy.isExpenseEligible(category)) {
                continue;
            }

            BigDecimal categoryTotal = safeMoney(entry.getValue());
            double categoryShare = divide(categoryTotal, analysisExpenseTotal);
            if (categoryShare <= ExpenseRecommendationThresholds.DOMINANT_CATEGORY_SHARE) {
                continue;
            }

            ExpenseCategoryProfile profile = ExpenseCategoryPolicy.resolveProfile(category);
            RecommendationPriority priority = categoryShare >= 0.45d
                    ? RecommendationPriority.HIGH
                    : RecommendationPriority.MEDIUM;
            BigDecimal estimatedGain = calculateDominanceEstimatedGain(categoryTotal, categoryShare);

            insights.add(ExpenseInsight.builder()
                    .insightType(CATEGORY_DOMINANCE)
                    .category(category)
                    .profile(profile)
                    .title(buildCategoryDominanceTitle(category, profile))
                    .message(buildCategoryDominanceMessage(category, profile, snapshot))
                    .suggestedAction(buildCategoryDominanceSuggestedAction(profile))
                    .priority(priority)
                    .severityScore(computeDominanceSeverity(categoryShare))
                    .confidenceScore(computeCategoryInsightConfidence(counts.getOrDefault(category, 0L), profile))
                    .estimatedMonthlyGain(toNullableDouble(estimatedGain))
                    .targetedTransactionsTotal(toNullableDouble(categoryTotal))
                    .explanation(String.format(
                            "La categorie %s represente %s de vos depenses %s (%s sur %s).",
                            categoryLabel(category),
                            formatPercent(categoryShare),
                            analysisPeriodDescriptor(snapshot),
                            formatMoney(categoryTotal),
                            formatMoney(analysisExpenseTotal)
                    ))
                    .basedOn(List.of(
                            "Seuil de reference: " + formatPercent(ExpenseRecommendationThresholds.DOMINANT_CATEGORY_SHARE)
                                    + " des depenses observees " + analysisPeriodDescriptor(snapshot),
                            observedTransactionsLabel(snapshot) + counts.getOrDefault(category, 0L),
                            analyzedPeriodLabel(snapshot)
                    ))
                    .build());
        }

        return insights;
    }

    private List<ExpenseInsight> detectMonthlyTotalSpike(ExpenseMetricsSnapshot snapshot) {
        BigDecimal analysisExpenseTotal = safeMoney(snapshot.getAnalysisExpenseTotal());
        BigDecimal baselineMonthlyAverage = safeMoney(snapshot.getBaselineMonthlyAverage());

        if (analysisExpenseTotal.signum() <= 0 || baselineMonthlyAverage.signum() <= 0) {
            return List.of();
        }

        BigDecimal triggerThreshold = baselineMonthlyAverage.multiply(
                BigDecimal.valueOf(ExpenseRecommendationThresholds.MONTHLY_TOTAL_SPIKE_RATIO)
        );
        if (analysisExpenseTotal.compareTo(triggerThreshold) <= 0) {
            return List.of();
        }

        double totalRatio = divide(analysisExpenseTotal, baselineMonthlyAverage);
        RecommendationPriority priority = totalRatio >= 1.25d
                ? RecommendationPriority.HIGH
                : RecommendationPriority.MEDIUM;

        return List.of(ExpenseInsight.builder()
                .insightType(MONTHLY_TOTAL_SPIKE)
                .category(null)
                .profile(null)
                .title("Surveiller la hausse globale des depenses")
                .message("Vos depenses globales sont superieures a votre rythme recent.")
                .suggestedAction("Identifier les postes qui ont le plus contribue a la hausse sur la periode analysee.")
                .priority(priority)
                .severityScore(computeMonthlyTotalSeverity(totalRatio))
                .confidenceScore(84.0d)
                .estimatedMonthlyGain(toNullableDouble(positiveDifference(analysisExpenseTotal, baselineMonthlyAverage)))
                .explanation(String.format(
                        "Le total des depenses %s atteint %s contre une moyenne recente de %s.",
                        analysisPeriodDescriptor(snapshot),
                        formatMoney(analysisExpenseTotal),
                        formatMoney(baselineMonthlyAverage)
                ))
                .basedOn(List.of(
                        "Seuil de declenchement: " + formatMoney(triggerThreshold)
                                + " (" + formatPercent(ExpenseRecommendationThresholds.MONTHLY_TOTAL_SPIKE_RATIO) + " de la moyenne recente)",
                        analyzedPeriodLabel(snapshot)
                ))
                .build());
    }

    private List<ExpenseInsight> detectFixedChargesPressure(ExpenseMetricsSnapshot snapshot) {
        BigDecimal fixedChargesTotal = safeMoney(snapshot.getFixedChargesTotal());
        BigDecimal analysisIncomeTotal = safeMoney(snapshot.getAnalysisIncomeTotal());

        if (fixedChargesTotal.signum() <= 0 || analysisIncomeTotal.signum() <= 0) {
            return List.of();
        }

        double fixedChargesRatio = divide(fixedChargesTotal, analysisIncomeTotal);
        if (fixedChargesRatio <= ExpenseRecommendationThresholds.FIXED_CHARGES_INCOME_RATIO) {
            return List.of();
        }

        RecommendationPriority priority = fixedChargesRatio >= 0.80d
                ? RecommendationPriority.HIGH
                : RecommendationPriority.MEDIUM;
        BigDecimal referenceAmount = analysisIncomeTotal.multiply(
                BigDecimal.valueOf(ExpenseRecommendationThresholds.FIXED_CHARGES_INCOME_RATIO)
        );

        return List.of(ExpenseInsight.builder()
                .insightType(FIXED_CHARGES_PRESSURE)
                .category(null)
                .profile(ExpenseCategoryProfile.FIXED_STRUCTURAL)
                .title("Reduire la pression des charges fixes")
                .message("Vos depenses de logement et factures limitent fortement votre marge mensuelle.")
                .suggestedAction("Identifier les charges incompressibles et les postes eventuellement renegociables.")
                .priority(priority)
                .severityScore(computeFixedChargesSeverity(fixedChargesRatio))
                .confidenceScore(88.0d)
                .estimatedMonthlyGain(toNullableDouble(positiveDifference(fixedChargesTotal, referenceAmount)))
                .explanation(String.format(
                        "Les charges fixes %s atteignent %s pour un revenu de %s.",
                        analysisPeriodQualifier(snapshot),
                        formatMoney(fixedChargesTotal),
                        formatMoney(analysisIncomeTotal)
                ))
                .basedOn(List.of(
                        "Ratio charges fixes / revenus: " + formatPercent(fixedChargesRatio),
                        "Seuil de reference: " + formatPercent(ExpenseRecommendationThresholds.FIXED_CHARGES_INCOME_RATIO),
                        analyzedPeriodLabel(snapshot)
                ))
                .build());
    }

    private String buildCategorySpikeTitle(TransactionCategory category, ExpenseCategoryProfile profile) {
        switch (profile) {
            case DISCRETIONARY:
                return "Maitriser les depenses " + categoryLabel(category);
            case ESSENTIAL_VARIABLE:
                return "Mieux cadrer le budget " + categoryLabel(category);
            case FIXED_STRUCTURAL:
                return "Surveiller la hausse des charges fixes";
            case AMBIGUOUS:
            default:
                return "Surveiller certaines depenses peu qualifiees";
        }
    }

    private String buildCategorySpikeMessage(TransactionCategory category, ExpenseCategoryProfile profile) {
        switch (profile) {
            case DISCRETIONARY:
                return "Les depenses de " + categoryLabel(category) + " depassent nettement votre rythme recent.";
            case ESSENTIAL_VARIABLE:
                return "Le budget " + categoryLabel(category) + " depasse votre moyenne recente.";
            case FIXED_STRUCTURAL:
                return "Les charges liees a " + categoryLabel(category) + " augmentent par rapport a votre rythme recent.";
            case AMBIGUOUS:
            default:
                return "Certaines depenses peu qualifiees progressent au-dessus de votre moyenne recente.";
        }
    }

    private String buildCategorySpikeSuggestedAction(ExpenseCategoryProfile profile) {
        switch (profile) {
            case DISCRETIONARY:
                return "Identifier les achats reportables et fixer un plafond sur cette categorie.";
            case ESSENTIAL_VARIABLE:
                return "Revoir les usages et definir un plafond de depense realiste pour cette categorie.";
            case FIXED_STRUCTURAL:
                return "Verifier les hausses recentes et les postes eventuellement renegociables.";
            case AMBIGUOUS:
            default:
                return "Requalifier ces transactions pour mieux suivre ce poste et verifier les montants inhabituels.";
        }
    }

    private String buildCategoryDominanceTitle(TransactionCategory category, ExpenseCategoryProfile profile) {
        switch (profile) {
            case DISCRETIONARY:
                return "Limiter le poids du " + categoryLabel(category) + " dans votre budget";
            case ESSENTIAL_VARIABLE:
                return "Mieux equilibrer le budget " + categoryLabel(category);
            case FIXED_STRUCTURAL:
                return "Surveiller le poids de " + categoryLabel(category) + " dans votre budget";
            case AMBIGUOUS:
            default:
                return "Mieux qualifier certaines depenses dominantes";
        }
    }

    private String buildCategoryDominanceMessage(
            TransactionCategory category,
            ExpenseCategoryProfile profile,
            ExpenseMetricsSnapshot snapshot
    ) {
        switch (profile) {
            case DISCRETIONARY:
                return "Une part importante de votre budget est absorbee par " + categoryLabel(category) + ".";
            case ESSENTIAL_VARIABLE:
                return "La categorie " + categoryLabel(category) + " occupe une place importante dans vos depenses "
                        + analysisPeriodDescriptor(snapshot) + ".";
            case FIXED_STRUCTURAL:
                return "Vos charges fixes pesent fortement sur votre marge mensuelle.";
            case AMBIGUOUS:
            default:
                return "Une categorie encore peu qualifiee occupe une part importante dans vos depenses "
                        + analysisPeriodDescriptor(snapshot) + ".";
        }
    }

    private String buildCategoryDominanceSuggestedAction(ExpenseCategoryProfile profile) {
        switch (profile) {
            case DISCRETIONARY:
                return "Fixer un budget cible sur cette categorie et suivre son evolution chaque semaine.";
            case ESSENTIAL_VARIABLE:
                return "Identifier les leviers de reduction sans degrader vos besoins essentiels.";
            case FIXED_STRUCTURAL:
                return "Distinguer les charges incompressibles des postes renegociables ou optimisables.";
            case AMBIGUOUS:
            default:
                return "Qualifier ces depenses pour identifier plus finement le poste a ajuster.";
        }
    }

    private double computeSpikeSeverity(double ratio) {
        return roundScore(clamp(45.0d + ((ratio - ExpenseRecommendationThresholds.CATEGORY_SPIKE_RATIO) * 140.0d)));
    }

    private double computeDominanceSeverity(double share) {
        return roundScore(clamp((share / ExpenseRecommendationThresholds.DOMINANT_CATEGORY_SHARE) * 55.0d));
    }

    private double computeMonthlyTotalSeverity(double ratio) {
        return roundScore(clamp(50.0d + ((ratio - ExpenseRecommendationThresholds.MONTHLY_TOTAL_SPIKE_RATIO) * 160.0d)));
    }

    private double computeFixedChargesSeverity(double ratio) {
        return roundScore(clamp((ratio / ExpenseRecommendationThresholds.FIXED_CHARGES_INCOME_RATIO) * 60.0d));
    }

    private BigDecimal calculateDominanceEstimatedGain(BigDecimal categoryTotal, double categoryShare) {
        BigDecimal safeCategoryTotal = safeMoney(categoryTotal);
        if (safeCategoryTotal.signum() <= 0
                || categoryShare <= ExpenseRecommendationThresholds.DOMINANT_CATEGORY_SHARE) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal estimatedGain = safeCategoryTotal.multiply(DOMINANCE_IMPACT_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        if (estimatedGain.compareTo(MIN_SIGNIFICANT_DOMINANCE_IMPACT) < 0
                && safeCategoryTotal.compareTo(MIN_SIGNIFICANT_CATEGORY_AMOUNT) >= 0) {
            return MIN_SIGNIFICANT_DOMINANCE_IMPACT;
        }

        return estimatedGain;
    }

    private double computeCategoryInsightConfidence(long count, ExpenseCategoryProfile profile) {
        double confidence = 72.0d + Math.min(12.0d, Math.max(0L, count - 1L) * 4.0d);
        if (profile != ExpenseCategoryProfile.AMBIGUOUS) {
            confidence += 6.0d;
        }
        return roundScore(clamp(confidence));
    }

    private Comparator<ExpenseInsight> buildInsightComparator() {
        return Comparator
                .comparingInt((ExpenseInsight insight) -> priorityRank(insight.getPriority()))
                .reversed()
                .thenComparing(ExpenseInsight::getSeverityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExpenseInsight::getInsightType, Comparator.nullsLast(String::compareTo));
    }

    private int priorityRank(RecommendationPriority priority) {
        if (priority == null) {
            return 0;
        }
        switch (priority) {
            case CRITICAL:
                return 4;
            case HIGH:
                return 3;
            case MEDIUM:
                return 2;
            case LOW:
            default:
                return 1;
        }
    }

    private Map<TransactionCategory, BigDecimal> safeMoneyMap(Map<TransactionCategory, BigDecimal> values) {
        if (values == null) {
            return new EnumMap<>(TransactionCategory.class);
        }
        return values;
    }

    private Map<TransactionCategory, Long> safeCountMap(Map<TransactionCategory, Long> values) {
        if (values == null) {
            return new EnumMap<>(TransactionCategory.class);
        }
        return values;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal positiveDifference(BigDecimal current, BigDecimal reference) {
        BigDecimal safeCurrent = safeMoney(current);
        BigDecimal safeReference = safeMoney(reference);
        if (safeCurrent.compareTo(safeReference) <= 0) {
            return BigDecimal.ZERO;
        }
        return safeCurrent.subtract(safeReference).setScale(2, RoundingMode.HALF_UP);
    }

    private double divide(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal safeDenominator = safeMoney(denominator);
        if (safeDenominator.signum() <= 0) {
            return 0.0d;
        }
        return safeMoney(numerator)
                .divide(safeDenominator, RATIO_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(100.0d, value));
    }

    private double roundScore(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String formatMoney(BigDecimal amount) {
        return safeMoney(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercent(double ratio) {
        return BigDecimal.valueOf(ratio * 100.0d)
                .setScale(1, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    private Double toNullableDouble(BigDecimal value) {
        BigDecimal safeValue = safeMoney(value);
        return safeValue.signum() > 0 ? safeValue.doubleValue() : null;
    }

    private String categoryLabel(TransactionCategory category) {
        if (category == null) {
            return "depenses";
        }
        return category.lowerLabel();
    }

    private String analysisPeriodDescriptor(ExpenseMetricsSnapshot snapshot) {
        return usesExtendedWindow(snapshot) ? "sur la periode recente analysee" : "sur les 30 derniers jours";
    }

    private String analysisPeriodQualifier(ExpenseMetricsSnapshot snapshot) {
        return usesExtendedWindow(snapshot) ? "sur la periode recente analysee" : "sur les 30 derniers jours";
    }

    private String observedTransactionsLabel(ExpenseMetricsSnapshot snapshot) {
        return usesExtendedWindow(snapshot)
                ? "Transactions observees sur la periode recente analysee: "
                : "Transactions observees sur les 30 derniers jours: ";
    }

    private String analyzedPeriodLabel(ExpenseMetricsSnapshot snapshot) {
        if (snapshot == null || snapshot.getAnalysisStartDate() == null || snapshot.getAnalysisEndDate() == null) {
            return "Periode analysee: recente";
        }
        return "Periode analysee: du " + snapshot.getAnalysisStartDate() + " au " + snapshot.getAnalysisEndDate();
    }

    private boolean usesExtendedWindow(ExpenseMetricsSnapshot snapshot) {
        return snapshot != null && snapshot.getAnalysisWindowDays() > DEFAULT_ANALYSIS_WINDOW_DAYS;
    }
}
