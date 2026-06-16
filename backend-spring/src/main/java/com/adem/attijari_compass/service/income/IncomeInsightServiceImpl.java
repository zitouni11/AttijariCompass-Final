package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomeClassifiedTransaction;
import com.adem.attijari_compass.dto.income.IncomeInsightResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class IncomeInsightServiceImpl implements IncomeInsightService {

    private static final String STABLE = "STABLE";
    private static final String MODERATE = "MODERATE";
    private static final String VARIABLE = "VARIABLE";
    private static final String UNDEFINED = "UNDEFINED";
    private static final String MONTHLY = "MONTHLY";
    private static final String IRREGULAR = "IRREGULAR";
    private static final String UNKNOWN_REGULARITY = "UNKNOWN";
    private static final int MIN_DATA_POINTS = 3;

    @Override
    public IncomeInsightResponse analyze(List<IncomeClassifiedTransaction> incomes) {
        List<IncomeClassifiedTransaction> sanitizedIncomes = sanitizeIncomes(incomes);
        Map<String, TypeStats> statsByType = buildTypeStats(sanitizedIncomes);

        int salaryLikeCount = countByType(statsByType, IncomeTypes.SALAIRE);
        int freelanceLikeCount = countByType(statsByType, IncomeTypes.FREELANCE);
        int transferLikeCount = countByType(statsByType, IncomeTypes.TRANSFER);
        int cashDepositLikeCount = countByType(statsByType, IncomeTypes.CASH_DEPOSIT);
        int otherIncomeCount = sanitizedIncomes.size()
                - salaryLikeCount
                - freelanceLikeCount
                - transferLikeCount
                - cashDepositLikeCount;

        String primaryIncomeType = determinePrimaryIncomeType(statsByType);
        double dominantIncomeShare = computeDominantIncomeShare(statsByType, primaryIncomeType);
        String incomeRegularity = determineIncomeRegularity(sanitizedIncomes, primaryIncomeType);
        double averageConfidence = computeAverageConfidence(sanitizedIncomes, primaryIncomeType);
        boolean hasSecondaryIncome = hasSecondaryIncome(statsByType, primaryIncomeType);
        String incomeStability = determineIncomeStability(
                sanitizedIncomes,
                statsByType,
                primaryIncomeType,
                dominantIncomeShare,
                averageConfidence,
                incomeRegularity,
                hasSecondaryIncome
        );
        int incomeConfidenceScore = computeIncomeConfidenceScore(
                primaryIncomeType,
                dominantIncomeShare,
                averageConfidence,
                incomeStability,
                incomeRegularity
        );

        return new IncomeInsightResponse(
                primaryIncomeType,
                incomeStability,
                incomeRegularity,
                incomeConfidenceScore,
                salaryLikeCount,
                freelanceLikeCount,
                transferLikeCount,
                cashDepositLikeCount,
                Math.max(otherIncomeCount, 0),
                roundShare(dominantIncomeShare),
                hasSecondaryIncome,
                buildInsightSummary(
                        sanitizedIncomes.size(),
                        primaryIncomeType,
                        incomeStability,
                        incomeRegularity,
                        hasSecondaryIncome,
                        dominantIncomeShare,
                        determineSecondaryIncomeType(statsByType, primaryIncomeType)
                )
        );
    }

    private List<IncomeClassifiedTransaction> sanitizeIncomes(List<IncomeClassifiedTransaction> incomes) {
        if (incomes == null || incomes.isEmpty()) {
            return List.of();
        }

        List<IncomeClassifiedTransaction> sanitized = new ArrayList<>();
        for (IncomeClassifiedTransaction income : incomes) {
            if (income == null) {
                continue;
            }

            IncomeClassifiedTransaction sanitizedIncome = new IncomeClassifiedTransaction();
            sanitizedIncome.setType(IncomeTypes.normalize(income.getType()));
            sanitizedIncome.setConfidence(clamp(income.getConfidence()));
            sanitizedIncome.setAmount(safeAmount(income.getAmount()));
            sanitizedIncome.setDate(income.getDate());
            sanitizedIncome.setSource(income.getSource());
            sanitized.add(sanitizedIncome);
        }

        return sanitized;
    }

    private Map<String, TypeStats> buildTypeStats(List<IncomeClassifiedTransaction> incomes) {
        Map<String, TypeStats> statsByType = new HashMap<>();
        for (IncomeClassifiedTransaction income : incomes) {
            String type = IncomeTypes.normalize(income.getType());
            TypeStats stats = statsByType.computeIfAbsent(type, ignored -> new TypeStats());
            stats.count++;
            stats.totalConfidence += income.getConfidence();
            stats.totalAmount = stats.totalAmount.add(safeAmount(income.getAmount()));
            stats.weightedScore += 1.0d + (income.getConfidence() * 0.35d);
        }
        return statsByType;
    }

    private int countByType(Map<String, TypeStats> statsByType, String type) {
        TypeStats stats = statsByType.get(type);
        return stats != null ? stats.count : 0;
    }

    private String determinePrimaryIncomeType(Map<String, TypeStats> statsByType) {
        if (statsByType.isEmpty()) {
            return IncomeTypes.UNKNOWN;
        }

        List<Map.Entry<String, TypeStats>> candidates = statsByType.entrySet().stream()
                .filter(entry -> !IncomeTypes.UNKNOWN.equals(entry.getKey()))
                .toList();

        if (candidates.isEmpty()) {
            return IncomeTypes.UNKNOWN;
        }

        return candidates.stream()
                .max(Comparator
                        .comparingDouble((Map.Entry<String, TypeStats> entry) -> entry.getValue().weightedScore)
                        .thenComparingInt(entry -> entry.getValue().count)
                        .thenComparingDouble(entry -> entry.getValue().totalConfidence))
                .map(Map.Entry::getKey)
                .orElse(IncomeTypes.UNKNOWN);
    }

    private double computeDominantIncomeShare(Map<String, TypeStats> statsByType, String primaryIncomeType) {
        if (statsByType.isEmpty()) {
            return 0.0d;
        }

        int totalUsableCount = statsByType.entrySet().stream()
                .filter(entry -> !IncomeTypes.UNKNOWN.equals(entry.getKey()))
                .mapToInt(entry -> entry.getValue().count)
                .sum();

        int denominator = totalUsableCount > 0 ? totalUsableCount : statsByType.values().stream()
                .mapToInt(stats -> stats.count)
                .sum();

        if (denominator <= 0) {
            return 0.0d;
        }

        TypeStats primaryStats = statsByType.get(primaryIncomeType);
        if (primaryStats == null) {
            return 0.0d;
        }

        return primaryStats.count / (double) denominator;
    }

    private String determineIncomeRegularity(List<IncomeClassifiedTransaction> incomes, String primaryIncomeType) {
        if (incomes.isEmpty() || IncomeTypes.UNKNOWN.equals(primaryIncomeType)) {
            return UNKNOWN_REGULARITY;
        }

        List<LocalDate> dates = incomes.stream()
                .filter(Objects::nonNull)
                .filter(income -> primaryIncomeType.equals(IncomeTypes.normalize(income.getType())))
                .map(IncomeClassifiedTransaction::getDate)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        if (dates.size() < MIN_DATA_POINTS) {
            return UNKNOWN_REGULARITY;
        }

        int monthlyGaps = 0;
        int totalGaps = dates.size() - 1;
        for (int index = 1; index < dates.size(); index++) {
            long gapDays = ChronoUnit.DAYS.between(dates.get(index - 1), dates.get(index));
            if (gapDays >= 25 && gapDays <= 35) {
                monthlyGaps++;
            }
        }

        if (totalGaps <= 0) {
            return UNKNOWN_REGULARITY;
        }

        return monthlyGaps / (double) totalGaps >= 0.60d ? MONTHLY : IRREGULAR;
    }

    private double computeAverageConfidence(List<IncomeClassifiedTransaction> incomes, String primaryIncomeType) {
        List<IncomeClassifiedTransaction> scopedIncomes = incomes.stream()
                .filter(Objects::nonNull)
                .filter(income -> shouldIncludeForConfidence(income, primaryIncomeType))
                .toList();

        if (scopedIncomes.isEmpty()) {
            return 0.0d;
        }

        return scopedIncomes.stream()
                .mapToDouble(IncomeClassifiedTransaction::getConfidence)
                .average()
                .orElse(0.0d);
    }

    private boolean shouldIncludeForConfidence(IncomeClassifiedTransaction income, String primaryIncomeType) {
        String normalizedType = IncomeTypes.normalize(income.getType());
        if (!IncomeTypes.UNKNOWN.equals(primaryIncomeType)) {
            return !IncomeTypes.UNKNOWN.equals(normalizedType);
        }

        return true;
    }

    private boolean hasSecondaryIncome(Map<String, TypeStats> statsByType, String primaryIncomeType) {
        if (statsByType.isEmpty() || IncomeTypes.UNKNOWN.equals(primaryIncomeType)) {
            return false;
        }

        int usableCount = statsByType.entrySet().stream()
                .filter(entry -> !IncomeTypes.UNKNOWN.equals(entry.getKey()))
                .mapToInt(entry -> entry.getValue().count)
                .sum();

        if (usableCount <= 1) {
            return false;
        }

        return statsByType.entrySet().stream()
                .filter(entry -> !primaryIncomeType.equals(entry.getKey()))
                .filter(entry -> !IncomeTypes.UNKNOWN.equals(entry.getKey()))
                .anyMatch(entry -> entry.getValue().count >= 2
                        || (entry.getValue().count / (double) usableCount) >= 0.25d);
    }

    private String determineSecondaryIncomeType(Map<String, TypeStats> statsByType, String primaryIncomeType) {
        return statsByType.entrySet().stream()
                .filter(entry -> !primaryIncomeType.equals(entry.getKey()))
                .filter(entry -> !IncomeTypes.UNKNOWN.equals(entry.getKey()))
                .max(Comparator
                        .comparingInt((Map.Entry<String, TypeStats> entry) -> entry.getValue().count)
                        .thenComparingDouble(entry -> entry.getValue().weightedScore))
                .map(Map.Entry::getKey)
                .orElse(IncomeTypes.UNKNOWN);
    }

    private String determineIncomeStability(List<IncomeClassifiedTransaction> incomes,
                                            Map<String, TypeStats> statsByType,
                                            String primaryIncomeType,
                                            double dominantIncomeShare,
                                            double averageConfidence,
                                            String incomeRegularity,
                                            boolean hasSecondaryIncome) {
        int usableCount = statsByType.entrySet().stream()
                .filter(entry -> !IncomeTypes.UNKNOWN.equals(entry.getKey()))
                .mapToInt(entry -> entry.getValue().count)
                .sum();

        if (incomes.size() < MIN_DATA_POINTS || usableCount < MIN_DATA_POINTS || IncomeTypes.UNKNOWN.equals(primaryIncomeType)) {
            return UNDEFINED;
        }

        if (IncomeTypes.SALAIRE.equals(primaryIncomeType)
                && dominantIncomeShare >= 0.60d
                && averageConfidence >= 0.70d
                && (MONTHLY.equals(incomeRegularity) || countByType(statsByType, primaryIncomeType) >= MIN_DATA_POINTS)) {
            return STABLE;
        }

        if (IncomeTypes.FREELANCE.equals(primaryIncomeType)
                || IncomeTypes.TRANSFER.equals(primaryIncomeType)
                || IncomeTypes.CASH_DEPOSIT.equals(primaryIncomeType)) {
            return VARIABLE;
        }

        if (hasSecondaryIncome || dominantIncomeShare < 0.65d || IRREGULAR.equals(incomeRegularity)) {
            return MODERATE;
        }

        return MODERATE;
    }

    private int computeIncomeConfidenceScore(String primaryIncomeType,
                                             double dominantIncomeShare,
                                             double averageConfidence,
                                             String incomeStability,
                                             String incomeRegularity) {
        if (IncomeTypes.UNKNOWN.equals(primaryIncomeType)) {
            return 0;
        }

        double score = (dominantIncomeShare * 40.0d)
                + (averageConfidence * 35.0d)
                + stabilityBonus(incomeStability)
                + regularityBonus(incomeRegularity);

        if (UNDEFINED.equals(incomeStability)) {
            score = Math.min(score, 45.0d);
        }

        return (int) Math.round(clamp(score / 100.0d) * 100.0d);
    }

    private double stabilityBonus(String incomeStability) {
        return switch (incomeStability) {
            case STABLE -> 15.0d;
            case MODERATE -> 10.0d;
            case VARIABLE -> 6.0d;
            default -> 0.0d;
        };
    }

    private double regularityBonus(String incomeRegularity) {
        return switch (incomeRegularity) {
            case MONTHLY -> 10.0d;
            case IRREGULAR -> 5.0d;
            default -> 0.0d;
        };
    }

    private String buildInsightSummary(int totalCount,
                                       String primaryIncomeType,
                                       String incomeStability,
                                       String incomeRegularity,
                                       boolean hasSecondaryIncome,
                                       double dominantIncomeShare,
                                       String secondaryIncomeType) {
        if (totalCount < MIN_DATA_POINTS || IncomeTypes.UNKNOWN.equals(primaryIncomeType)) {
            return "Les donnees disponibles sont encore insuffisantes pour caracteriser precisement vos revenus.";
        }

        if (STABLE.equals(incomeStability)
                && MONTHLY.equals(incomeRegularity)
                && IncomeTypes.SALAIRE.equals(primaryIncomeType)) {
            return "Vos revenus semblent principalement stables et domines par un salaire mensuel.";
        }

        if (VARIABLE.equals(incomeStability)) {
            if (hasSecondaryIncome && !IncomeTypes.UNKNOWN.equals(secondaryIncomeType)) {
                return "Vos revenus paraissent variables, avec une forte presence de "
                        + describeTypePlural(primaryIncomeType)
                        + " et une source secondaire notable en "
                        + describeTypeSingular(secondaryIncomeType)
                        + ".";
            }

            return "Vos revenus paraissent variables, principalement portes par "
                    + describeTypePlural(primaryIncomeType)
                    + ".";
        }

        if (hasSecondaryIncome) {
            return "Vos revenus semblent moderement stables, principalement portes par "
                    + describeTypeSingular(primaryIncomeType)
                    + ", avec une source secondaire notable.";
        }

        if (dominantIncomeShare >= 0.60d) {
            return "Vos revenus semblent principalement structures autour de "
                    + describeTypeSingular(primaryIncomeType)
                    + ".";
        }

        return "Vos revenus presentent un profil mixte qui reste encore a consolider.";
    }

    private String describeTypeSingular(String type) {
        return switch (IncomeTypes.normalize(type)) {
            case IncomeTypes.SALAIRE -> "un salaire";
            case IncomeTypes.FREELANCE -> "une activite freelance";
            case IncomeTypes.TRANSFER -> "des virements recus";
            case IncomeTypes.CASH_DEPOSIT -> "des depots d'especes";
            case IncomeTypes.LOYER -> "des loyers";
            default -> "des revenus difficiles a qualifier";
        };
    }

    private String describeTypePlural(String type) {
        return switch (IncomeTypes.normalize(type)) {
            case IncomeTypes.SALAIRE -> "des salaires";
            case IncomeTypes.FREELANCE -> "des revenus freelance";
            case IncomeTypes.TRANSFER -> "des virements recus";
            case IncomeTypes.CASH_DEPOSIT -> "des depots d'especes";
            case IncomeTypes.LOYER -> "des loyers";
            default -> "des revenus difficiles a qualifier";
        };
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount.abs() : BigDecimal.ZERO;
    }

    private double roundShare(double dominantIncomeShare) {
        return BigDecimal.valueOf(dominantIncomeShare)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(value, 1.0d));
    }

    private static final class TypeStats {
        private int count;
        private double totalConfidence;
        private double weightedScore;
        private BigDecimal totalAmount = BigDecimal.ZERO;
    }
}
