package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.goal.GoalAnalysisResponse;
import com.adem.attijari_compass.dto.goal.GoalBlockingCategoryResponse;
import com.adem.attijari_compass.entity.FinancialGoal;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalAnalysisService {

    private static final int MAX_ANALYSIS_MONTHS = 6;
    private static final double MIN_FALLBACK_CAPACITY = 50.0;
    private static final double BALANCED_FALLBACK_CAPACITY = 75.0;
    private static final double AGGRESSIVE_FALLBACK_CAPACITY = 100.0;
    private static final Map<TransactionCategory, Double> REDUCTION_RATES = createReductionRates();
    private static final Map<TransactionCategory, Double> DEFICIT_REVIEW_RATES = createDeficitReviewRates();
    private static final List<TransactionCategory> REPORTED_FIXED_EXPENSE_CATEGORIES = List.of(
            TransactionCategory.LOGEMENT,
            TransactionCategory.OPERATEURS_TELEPHONIQUES,
            TransactionCategory.FACTURES,
            TransactionCategory.STEG_SONEDE,
            TransactionCategory.BANQUE,
            TransactionCategory.EDUCATION
    );
    private static final List<TransactionCategory> COMPRESSIBLE_EXPENSE_CATEGORIES = List.of(
            TransactionCategory.RESTAURANT,
            TransactionCategory.CAFES,
            TransactionCategory.LIVRAISON,
            TransactionCategory.DIVERTISSEMENT,
            TransactionCategory.HOTEL,
            TransactionCategory.VOYAGE,
            TransactionCategory.SHOPPING,
            TransactionCategory.BEAUTE
    );
    private static final List<TransactionCategory> FIXED_EXPENSE_CATEGORIES = List.of(
            TransactionCategory.LOGEMENT,
            TransactionCategory.FACTURES,
            TransactionCategory.OPERATEURS_TELEPHONIQUES,
            TransactionCategory.STEG_SONEDE,
            TransactionCategory.TECHNOLOGIE,
            TransactionCategory.EDUCATION,
            TransactionCategory.BANQUE
    );

    private final TransactionRepository transactionRepository;

    public GoalAnalysisResponse getGoalAnalysis(FinancialGoal goal) {
        return mapToResponse(goal.getId(), analyze(goal));
    }

    GoalAnalysisResponse mapToResponse(Long goalId, GoalAnalysisSnapshot snapshot) {
        return GoalAnalysisResponse.builder()
                .goalId(goalId)
                .monthsAnalyzed(snapshot.monthsAnalyzed())
                .transactionCount(snapshot.transactionCount())
                .enoughData(snapshot.enoughData())
                .analysisMessage(snapshot.analysisMessage())
                .averageMonthlyIncome(snapshot.averageMonthlyIncome())
                .averageMonthlyExpenses(snapshot.averageMonthlyExpenses())
                .fixedExpenses(snapshot.fixedExpenses())
                .estimatedFixedExpenses(snapshot.estimatedFixedExpenses())
                .estimatedEssentialExpenses(snapshot.estimatedEssentialExpenses())
                .compressibleExpenses(snapshot.compressibleExpenses())
                .estimatedCompressibleExpenses(snapshot.estimatedCompressibleExpenses())
                .prudentSavingCapacity(snapshot.prudentSavingCapacity())
                .balancedSavingCapacity(snapshot.balancedSavingCapacity())
                .aggressiveSavingCapacity(snapshot.aggressiveSavingCapacity())
                .averageHistoricalSavings(snapshot.averageHistoricalSavings())
                .incomeStabilityScore(snapshot.incomeStabilityScore())
                .expenseStabilityScore(snapshot.expenseStabilityScore())
                .blockingCategories(snapshot.blockingCategories())
                .build();
    }

    public GoalAnalysisSnapshot analyze(FinancialGoal goal) {
        List<Transaction> allTransactions = transactionRepository.findAllByUserId(goal.getUser().getId());
        List<Transaction> usableTransactions = allTransactions.stream()
                .filter(transaction -> transaction.getDate() != null)
                .toList();

        if (usableTransactions.isEmpty()) {
            return new GoalAnalysisSnapshot(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    MIN_FALLBACK_CAPACITY,
                    BALANCED_FALLBACK_CAPACITY,
                    AGGRESSIVE_FALLBACK_CAPACITY,
                    0.0,
                    0.0,
                    0.0,
                    1,
                    0,
                    false,
                    "Historique insuffisant pour estimer une capacite d'epargne fiable.",
                    List.of()
            );
        }

        YearMonth latestTransactionMonth = usableTransactions.stream()
                .map(Transaction::getDate)
                .max(LocalDate::compareTo)
                .map(YearMonth::from)
                .orElse(YearMonth.from(LocalDate.now()));
        YearMonth earliestRelevantMonth = latestTransactionMonth.minusMonths(MAX_ANALYSIS_MONTHS - 1L);
        YearMonth firstTransactionMonth = usableTransactions.stream()
                .map(Transaction::getDate)
                .min(LocalDate::compareTo)
                .map(YearMonth::from)
                .orElse(latestTransactionMonth);
        YearMonth startMonth = firstTransactionMonth.isAfter(earliestRelevantMonth) ? firstTransactionMonth : earliestRelevantMonth;
        List<YearMonth> analysisMonths = buildMonthRange(startMonth, latestTransactionMonth);

        Map<YearMonth, Double> monthlyIncome = new java.util.LinkedHashMap<>();
        Map<YearMonth, Double> monthlyExpenses = new java.util.LinkedHashMap<>();
        for (YearMonth month : analysisMonths) {
            monthlyIncome.put(month, 0.0);
            monthlyExpenses.put(month, 0.0);
        }

        EnumMap<TransactionCategory, Double> expenseTotalsByCategory = new EnumMap<>(TransactionCategory.class);
        for (TransactionCategory category : TransactionCategory.values()) {
            expenseTotalsByCategory.put(category, 0.0);
        }

        for (Transaction transaction : usableTransactions) {
            YearMonth month = YearMonth.from(transaction.getDate());
            if (month.isBefore(startMonth) || month.isAfter(latestTransactionMonth)) {
                continue;
            }

            if (transaction.getType() == TransactionType.REVENU) {
                monthlyIncome.put(month, monthlyIncome.getOrDefault(month, 0.0) + safeAmount(transaction.getAmount()));
                continue;
            }

            double amount = safeAmount(transaction.getAmount());
            monthlyExpenses.put(month, monthlyExpenses.getOrDefault(month, 0.0) + amount);
            expenseTotalsByCategory.put(transaction.getCategory(),
                    expenseTotalsByCategory.getOrDefault(transaction.getCategory(), 0.0) + amount);
        }

        int monthsAnalyzed = analysisMonths.size();
        int transactionCount = (int) usableTransactions.stream()
                .filter(transaction -> {
                    YearMonth month = YearMonth.from(transaction.getDate());
                    return !month.isBefore(startMonth) && !month.isAfter(latestTransactionMonth);
                })
                .count();

        double averageMonthlyIncome = round(sum(monthlyIncome.values()) / monthsAnalyzed);
        double averageMonthlyExpenses = round(sum(monthlyExpenses.values()) / monthsAnalyzed);
        Map<TransactionCategory, Double> averageExpenseByCategory = new EnumMap<>(TransactionCategory.class);
        for (TransactionCategory category : TransactionCategory.values()) {
            averageExpenseByCategory.put(category, round(expenseTotalsByCategory.getOrDefault(category, 0.0) / monthsAnalyzed));
        }

        double fixedExpenses = round(sumCategories(averageExpenseByCategory, REPORTED_FIXED_EXPENSE_CATEGORIES));
        double estimatedFixedExpenses = round(sumCategories(averageExpenseByCategory, FIXED_EXPENSE_CATEGORIES));
        double essentialTransport = averageExpenseByCategory.getOrDefault(TransactionCategory.TRANSPORT, 0.0) * 0.70;
        double estimatedEssentialExpenses = round(
                estimatedFixedExpenses
                        + averageExpenseByCategory.getOrDefault(TransactionCategory.LOGEMENT, 0.0)
                        + averageExpenseByCategory.getOrDefault(TransactionCategory.ALIMENTATION, 0.0)
                        + averageExpenseByCategory.getOrDefault(TransactionCategory.SUPERMARCHE, 0.0)
                        + averageExpenseByCategory.getOrDefault(TransactionCategory.DISTRIBUTION, 0.0)
                        + averageExpenseByCategory.getOrDefault(TransactionCategory.SANTE, 0.0)
                        + essentialTransport
        );

        double compressibleExpenses = round(sumCategories(averageExpenseByCategory, COMPRESSIBLE_EXPENSE_CATEGORIES));
        double estimatedCompressibleExpenses = round(REDUCTION_RATES.entrySet().stream()
                .mapToDouble(entry -> averageExpenseByCategory.getOrDefault(entry.getKey(), 0.0) * entry.getValue())
                .sum());

        List<Double> monthlySavings = new ArrayList<>();
        for (YearMonth month : analysisMonths) {
            monthlySavings.add(monthlyIncome.getOrDefault(month, 0.0) - monthlyExpenses.getOrDefault(month, 0.0));
        }

        double averageHistoricalSavings = round(monthlySavings.stream()
                .mapToDouble(value -> Math.max(value, 0.0))
                .average()
                .orElse(0.0));
        double incomeStabilityScore = round(stabilityScore(new ArrayList<>(monthlyIncome.values())));
        double expenseStabilityScore = round(stabilityScore(new ArrayList<>(monthlyExpenses.values())));
        double historicalMonthlyRoom = Math.max(0.0, averageMonthlyIncome - averageMonthlyExpenses);
        double expenseVolatility = standardDeviation(new ArrayList<>(monthlyExpenses.values()));
        double safetyMargin = Math.max(averageMonthlyIncome * 0.10, expenseVolatility * 0.35);
        double baseCapacity = Math.max(0.0, averageMonthlyIncome - estimatedEssentialExpenses - safetyMargin);
        double effectiveBaseCapacity = Math.max(
                baseCapacity,
                Math.max(
                        Math.max(0.0, historicalMonthlyRoom - (safetyMargin * 0.20)),
                        averageHistoricalSavings * 0.75
                )
        );
        double stabilityFactor = 0.70 + ((incomeStabilityScore + expenseStabilityScore) / 200.0) * 0.30;

        boolean enoughData = monthsAnalyzed >= 2 && transactionCount >= 5 && averageMonthlyIncome > 0.0;
        boolean fallbackRequired = goal.getTargetAmount() > goal.getCurrentAmount()
                && (!enoughData || averageMonthlyExpenses > averageMonthlyIncome);

        double prudentSavingCapacity = round(Math.max(0.0,
                (effectiveBaseCapacity * 0.70 + averageHistoricalSavings * 0.20) * stabilityFactor));
        double balancedSavingCapacity = round(Math.max(prudentSavingCapacity,
                (effectiveBaseCapacity * 0.90 + estimatedCompressibleExpenses * 0.30) * stabilityFactor));
        double aggressiveSavingCapacity = round(Math.max(balancedSavingCapacity,
                (effectiveBaseCapacity + estimatedCompressibleExpenses * 0.55) * stabilityFactor));

        prudentSavingCapacity = applyCapacityFallback(prudentSavingCapacity, MIN_FALLBACK_CAPACITY, fallbackRequired);
        balancedSavingCapacity = applyCapacityFallback(Math.max(balancedSavingCapacity, prudentSavingCapacity),
                BALANCED_FALLBACK_CAPACITY, fallbackRequired);
        aggressiveSavingCapacity = applyCapacityFallback(Math.max(aggressiveSavingCapacity, balancedSavingCapacity),
                AGGRESSIVE_FALLBACK_CAPACITY, fallbackRequired);

        List<GoalBlockingCategoryResponse> blockingCategories = buildBlockingCategories(
                averageExpenseByCategory,
                averageMonthlyIncome,
                averageMonthlyExpenses
        );

        String analysisMessage = enoughData
                ? "Analyse construite a partir de l'historique recent et des categories de depenses."
                : "Analyse basee sur des donnees limitees. Les resultats restent indicatifs tant que plus de transactions ne sont pas disponibles.";

        log.debug("Goal analysis computed: goalId={}, averageMonthlyIncome={}, averageMonthlyExpenses={}, prudentSavingCapacity={}, balancedSavingCapacity={}, aggressiveSavingCapacity={}, blockingCategories={}",
                goal.getId(),
                averageMonthlyIncome,
                averageMonthlyExpenses,
                prudentSavingCapacity,
                balancedSavingCapacity,
                aggressiveSavingCapacity,
                blockingCategories.stream()
                        .map(GoalBlockingCategoryResponse::getDisplayLabel)
                        .toList());

        return new GoalAnalysisSnapshot(
                averageMonthlyIncome,
                averageMonthlyExpenses,
                fixedExpenses,
                estimatedFixedExpenses,
                estimatedEssentialExpenses,
                compressibleExpenses,
                estimatedCompressibleExpenses,
                prudentSavingCapacity,
                balancedSavingCapacity,
                aggressiveSavingCapacity,
                averageHistoricalSavings,
                incomeStabilityScore,
                expenseStabilityScore,
                monthsAnalyzed,
                transactionCount,
                enoughData,
                analysisMessage,
                blockingCategories
        );
    }

    private GoalBlockingCategoryResponse toBlockingCategory(
            TransactionCategory category,
            double reductionRate,
            Map<TransactionCategory, Double> averageExpenseByCategory,
            double averageMonthlyIncome
    ) {
        double averageMonthlyAmount = round(averageExpenseByCategory.getOrDefault(category, 0.0));
        double reducibleAmount = round(averageMonthlyAmount * reductionRate);
        double incomeShare = averageMonthlyIncome > 0.0 ? averageMonthlyAmount / averageMonthlyIncome : 0.0;
        String severity = severityFor(incomeShare, reducibleAmount);

        return GoalBlockingCategoryResponse.builder()
                .category(category)
                .categoryLabel(categoryLabel(category))
                .averageMonthlyAmount(averageMonthlyAmount)
                .estimatedReducibleAmount(reducibleAmount)
                .reductionRate(round(reductionRate * 100.0))
                .severity(severity)
                .severityLabel(severityLabel(severity))
                .displayLabel(buildBlockingDisplayLabel(category, severity))
                .reason(reasonFor(category, reductionRate))
                .build();
    }

    private List<GoalBlockingCategoryResponse> buildBlockingCategories(
            Map<TransactionCategory, Double> averageExpenseByCategory,
            double averageMonthlyIncome,
            double averageMonthlyExpenses
    ) {
        Map<TransactionCategory, Double> detectionRates = new EnumMap<>(TransactionCategory.class);
        detectionRates.putAll(REDUCTION_RATES);

        if (averageMonthlyExpenses > averageMonthlyIncome) {
            DEFICIT_REVIEW_RATES.forEach(detectionRates::putIfAbsent);
        }

        return detectionRates.entrySet().stream()
                .map(entry -> toBlockingCategory(entry.getKey(), entry.getValue(), averageExpenseByCategory, averageMonthlyIncome))
                .filter(blocking -> blocking.getAverageMonthlyAmount() > 0.0)
                .filter(blocking -> blocking.getEstimatedReducibleAmount() > 0.0)
                .sorted(Comparator.comparing(GoalBlockingCategoryResponse::getEstimatedReducibleAmount).reversed())
                .limit(4)
                .toList();
    }

    private double applyCapacityFallback(double capacity, double fallbackCapacity, boolean fallbackRequired) {
        if (!fallbackRequired) {
            return round(Math.max(0.0, capacity));
        }
        return round(Math.max(Math.max(0.0, capacity), fallbackCapacity));
    }

    private String severityFor(double incomeShare, double reducibleAmount) {
        if (incomeShare >= 0.15 || reducibleAmount >= 250.0) {
            return "HIGH";
        }
        if (incomeShare >= 0.08 || reducibleAmount >= 100.0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String reasonFor(TransactionCategory category, double reductionRate) {
        String baseReason = switch (category) {
            case RESTAURANT -> "Les depenses restaurant restent souvent compressibles quand vous regroupez les sorties et arbitrez mieux les repas hors domicile.";
            case CAFES -> "Les depenses de cafes peuvent souvent etre reduites sans toucher aux engagements fixes.";
            case LIVRAISON -> "Les depenses de livraison sont souvent fractionnees en petits montants mais restent compressibles.";
            case SHOPPING -> "Les depenses de shopping restent flexibles et peuvent etre ralenties pour soutenir cet objectif.";
            case DIVERTISSEMENT -> "Le divertissement offre une marge d'ajustement a court terme.";
            case HOTEL, VOYAGE -> "Les depenses liees au voyage et a l'hebergement temporaire peuvent etre replanifiees plus facilement que les besoins essentiels.";
            case LOGEMENT -> "Le logement est un poste structurel a suivre de pres, mais une renegociation ou un recadrage peut parfois liberer un peu de marge.";
            case BEAUTE -> "La categorie beaute peut etre cadencee differemment en periode de tension budgetaire.";
            case TRANSPORT -> "Une partie du budget transport peut parfois etre optimisee.";
            case ALIMENTATION, SUPERMARCHE, DISTRIBUTION -> "Le budget courses doit etre revu pour limiter les depassements evitables tout en preservant l'essentiel.";
            case OPERATEURS_TELEPHONIQUES -> "Les forfaits telecom merite une verification pour identifier des optimisations rapides.";
            case FACTURES -> "Les factures generales doivent etre revues pour identifier les postes repetitifs et les abonnements rattaches.";
            case STEG_SONEDE -> "Les charges d'eau et d'energie sont difficiles a reduire mais quelques leviers de sobriete existent.";
            case TECHNOLOGIE -> "Les services et abonnements technologiques doivent etre revises pour supprimer les couts recurrents peu utiles.";
            case SANTE -> "Les depenses de sante doivent etre analysees avec prudence pour distinguer le necessaire du reportable.";
            case EDUCATION -> "Les depenses d'education peuvent demander une priorisation temporaire quand le budget est sous tension.";
            case BANQUE -> "Les frais et flux bancaires doivent etre verifies pour confirmer que chaque sortie reste indispensable.";
            case EPARGNE -> "L'epargne n'est pas un poste de depense a compresser, mais un signal utile pour mesurer la capacite de mise de cote.";
            case SALAIRE -> "Le salaire represente une entree de fonds et ne doit pas etre traite comme une depense reductible.";
            case AUTRES -> "Les depenses non classees doivent etre revues transaction par transaction.";
            default -> "Cette categorie contient des depenses potentiellement reductibles au service de l'objectif.";
        };
        return baseReason + " Taux de reduction estime: " + round(reductionRate * 100.0) + "%.";
    }

    private static Map<TransactionCategory, Double> createReductionRates() {
        EnumMap<TransactionCategory, Double> rates = new EnumMap<>(TransactionCategory.class);
        rates.put(TransactionCategory.RESTAURANT, 0.50);
        rates.put(TransactionCategory.CAFES, 0.55);
        rates.put(TransactionCategory.LIVRAISON, 0.45);
        rates.put(TransactionCategory.SHOPPING, 0.50);
        rates.put(TransactionCategory.DIVERTISSEMENT, 0.45);
        rates.put(TransactionCategory.HOTEL, 0.40);
        rates.put(TransactionCategory.VOYAGE, 0.35);
        rates.put(TransactionCategory.BEAUTE, 0.30);
        rates.put(TransactionCategory.TRANSPORT, 0.20);
        rates.put(TransactionCategory.TECHNOLOGIE, 0.25);
        rates.put(TransactionCategory.AUTRES, 0.20);
        return Map.copyOf(rates);
    }

    private static Map<TransactionCategory, Double> createDeficitReviewRates() {
        EnumMap<TransactionCategory, Double> rates = new EnumMap<>(TransactionCategory.class);
        rates.put(TransactionCategory.ALIMENTATION, 0.15);
        rates.put(TransactionCategory.SUPERMARCHE, 0.12);
        rates.put(TransactionCategory.DISTRIBUTION, 0.10);
        rates.put(TransactionCategory.LOGEMENT, 0.08);
        rates.put(TransactionCategory.FACTURES, 0.10);
        rates.put(TransactionCategory.OPERATEURS_TELEPHONIQUES, 0.10);
        rates.put(TransactionCategory.STEG_SONEDE, 0.10);
        rates.put(TransactionCategory.SANTE, 0.08);
        rates.put(TransactionCategory.EDUCATION, 0.10);
        rates.put(TransactionCategory.BANQUE, 0.10);
        return Map.copyOf(rates);
    }

    private List<YearMonth> buildMonthRange(YearMonth startMonth, YearMonth endMonth) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return months;
    }

    private double stabilityScore(List<Double> values) {
        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (average <= 0.0) {
            return 0.0;
        }
        double coefficientOfVariation = standardDeviation(values) / average;
        return Math.max(0.0, 100.0 - Math.min(100.0, coefficientOfVariation * 100.0));
    }

    private double standardDeviation(List<Double> values) {
        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - average, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private double safeAmount(Double value) {
        return value != null ? value : 0.0;
    }

    private double sum(Iterable<Double> values) {
        double total = 0.0;
        for (Double value : values) {
            total += safeAmount(value);
        }
        return total;
    }

    private double sumCategories(Map<TransactionCategory, Double> averageExpenseByCategory, List<TransactionCategory> categories) {
        return categories.stream()
                .mapToDouble(category -> averageExpenseByCategory.getOrDefault(category, 0.0))
                .sum();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String categoryLabel(TransactionCategory category) {
        return category == null ? TransactionCategory.fallback().label() : category.label();
    }

    private String severityLabel(String severity) {
        return switch (severity) {
            case "HIGH" -> "Priorite elevee";
            case "MEDIUM" -> "Priorite moderee";
            default -> "Priorite secondaire";
        };
    }

    private String buildBlockingDisplayLabel(TransactionCategory category, String severity) {
        return categoryLabel(category) + " - " + severityLabel(severity);
    }

    public record GoalAnalysisSnapshot(
            double averageMonthlyIncome,
            double averageMonthlyExpenses,
            double fixedExpenses,
            double estimatedFixedExpenses,
            double estimatedEssentialExpenses,
            double compressibleExpenses,
            double estimatedCompressibleExpenses,
            double prudentSavingCapacity,
            double balancedSavingCapacity,
            double aggressiveSavingCapacity,
            double averageHistoricalSavings,
            double incomeStabilityScore,
            double expenseStabilityScore,
            int monthsAnalyzed,
            int transactionCount,
            boolean enoughData,
            String analysisMessage,
            List<GoalBlockingCategoryResponse> blockingCategories
    ) {
    }
}
