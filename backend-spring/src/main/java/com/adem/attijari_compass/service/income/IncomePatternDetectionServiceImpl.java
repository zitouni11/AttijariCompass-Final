package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomePatternDetectionResult;
import com.adem.attijari_compass.dto.income.IncomeTransactionSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
public class IncomePatternDetectionServiceImpl implements IncomePatternDetectionService {

    private static final String MONTHLY = "MONTHLY";
    private static final String NONE = "NONE";
    private static final int MIN_OCCURRENCES = 3;
    private static final double HIGH_STABILITY_THRESHOLD = 0.85d;
    private static final double MEDIUM_STABILITY_THRESHOLD = 0.65d;
    private static final double PATTERN_ACCEPTANCE_THRESHOLD = 0.65d;
    private static final double FREELANCE_MIN_STABILITY = 0.35d;
    private static final double FREELANCE_MAX_STABILITY = 0.80d;
    private static final double SALARY_KEYWORD_CONFIDENCE = 0.95d;
    private static final double TRANSFER_KEYWORD_CONFIDENCE = 0.85d;
    private static final double CASH_DEPOSIT_KEYWORD_CONFIDENCE = 0.80d;
    private static final double FREELANCE_KEYWORD_CONFIDENCE = 0.75d;

    @Override
    public IncomePatternDetectionResult detectPattern(IncomeTransactionSnapshot currentTransaction,
                                                      List<IncomeTransactionSnapshot> historicalCredits) {
        String currentSource = normalizeSource(currentTransaction);
        String currentKeywordSource = normalizeKeywordSource(currentTransaction);
        if (currentSource.isBlank()) {
            return unknown(
                    "PATTERN_SOURCE_MISSING",
                    "Impossible de detecter un pattern car la transaction ne contient pas de source exploitable.",
                    0,
                    false,
                    0.0d,
                    0.0d,
                    NONE,
                    0.0d
            );
        }

        List<IncomeTransactionSnapshot> similarOccurrences = collectSimilarOccurrences(
                currentTransaction,
                historicalCredits,
                currentSource
        );
        int occurrenceCount = similarOccurrences.size();
        double occurrenceScore = computeOccurrenceScore(occurrenceCount);
        double frequencyScore = computeFrequencyScore(similarOccurrences);
        boolean monthlyRecurring = isMonthlyRecurring(frequencyScore);
        String recurrenceType = monthlyRecurring ? MONTHLY : NONE;
        double amountStabilityScore = computeAmountStabilityScore(similarOccurrences);
        double sourceConsistencyScore = computeSourceConsistencyScore(currentSource, similarOccurrences);
        double patternConfidence = computePatternConfidence(
                occurrenceScore,
                frequencyScore,
                amountStabilityScore,
                sourceConsistencyScore
        );
        boolean mostlyEndOfMonth = isMostlyEndOfMonth(similarOccurrences);
        boolean mostlyBeginningOfMonth = isMostlyBeginningOfMonth(similarOccurrences);

        log.debug("Income pattern detection found {} similar occurrences for source '{}'",
                occurrenceCount, currentSource);

        if (matchesSalaryKeyword(currentKeywordSource)) {
            return result(
                    IncomeTypes.SALAIRE,
                    Math.max(patternConfidence, SALARY_KEYWORD_CONFIDENCE),
                    "RULE_SALARY_KEYWORD",
                    "Le libelle de la transaction contient des indices explicites de salaire ou de payroll.",
                    occurrenceCount,
                    monthlyRecurring,
                    amountStabilityScore,
                    sourceConsistencyScore,
                    recurrenceType
            );
        }

        if (monthlyRecurring
                && amountStabilityScore >= HIGH_STABILITY_THRESHOLD
                && patternConfidence >= PATTERN_ACCEPTANCE_THRESHOLD
                && mostlyEndOfMonth) {
            return result(
                    IncomeTypes.SALAIRE,
                    patternConfidence,
                    "PATTERN_RECURRING_STABLE_END_OF_MONTH",
                    buildPatternExplanation(
                            occurrenceCount,
                            true,
                            amountStabilityScore,
                            sourceConsistencyScore,
                            patternConfidence,
                            "Des credits mensuels stables apparaissent majoritairement en fin de mois, ce qui correspond a un salaire."
                    ),
                    occurrenceCount,
                    true,
                    amountStabilityScore,
                    sourceConsistencyScore,
                    MONTHLY
            );
        }

        if (monthlyRecurring
                && amountStabilityScore >= HIGH_STABILITY_THRESHOLD
                && patternConfidence >= PATTERN_ACCEPTANCE_THRESHOLD
                && mostlyBeginningOfMonth) {
            return result(
                    IncomeTypes.LOYER,
                    patternConfidence,
                    "PATTERN_RECURRING_STABLE_BEGINNING_OF_MONTH",
                    buildPatternExplanation(
                            occurrenceCount,
                            true,
                            amountStabilityScore,
                            sourceConsistencyScore,
                            patternConfidence,
                            "Des credits mensuels stables apparaissent majoritairement en debut de mois, ce qui correspond a un loyer."
                    ),
                    occurrenceCount,
                    true,
                    amountStabilityScore,
                    sourceConsistencyScore,
                    MONTHLY
            );
        }

        if (matchesCashDepositKeyword(currentKeywordSource)) {
            return result(
                    IncomeTypes.CASH_DEPOSIT,
                    Math.max(patternConfidence, CASH_DEPOSIT_KEYWORD_CONFIDENCE),
                    "RULE_CASH_DEPOSIT_KEYWORD",
                    "Le libelle de la transaction contient des indices explicites de depot d'especes ou de versement.",
                    occurrenceCount,
                    monthlyRecurring,
                    amountStabilityScore,
                    sourceConsistencyScore,
                    recurrenceType
            );
        }

        if (matchesTransferKeyword(currentKeywordSource)) {
            return result(
                    IncomeTypes.TRANSFER,
                    Math.max(patternConfidence, TRANSFER_KEYWORD_CONFIDENCE),
                    "RULE_TRANSFER_KEYWORD",
                    "Le libelle de la transaction contient des indices explicites de virement ou transfert recu.",
                    occurrenceCount,
                    monthlyRecurring,
                    amountStabilityScore,
                    sourceConsistencyScore,
                    recurrenceType
            );
        }

        if (matchesFreelanceKeyword(currentKeywordSource)) {
            return result(
                    IncomeTypes.FREELANCE,
                    Math.max(patternConfidence, FREELANCE_KEYWORD_CONFIDENCE),
                    "RULE_FREELANCE_KEYWORD",
                    "Le libelle de la transaction contient des indices explicites de mission, prestation ou activite freelance.",
                    occurrenceCount,
                    monthlyRecurring,
                    amountStabilityScore,
                    sourceConsistencyScore,
                    recurrenceType
            );
        }

        if (shouldClassifyAsFreelance(
                currentKeywordSource,
                occurrenceCount,
                amountStabilityScore,
                sourceConsistencyScore,
                patternConfidence,
                monthlyRecurring
        )) {
            return result(
                    IncomeTypes.FREELANCE,
                    Math.max(patternConfidence, PATTERN_ACCEPTANCE_THRESHOLD),
                    "PATTERN_RECURRING_VARIABLE_FREELANCE",
                    buildPatternExplanation(
                            occurrenceCount,
                            monthlyRecurring,
                            amountStabilityScore,
                            sourceConsistencyScore,
                            Math.max(patternConfidence, PATTERN_ACCEPTANCE_THRESHOLD),
                            "Des revenus recurrents sont detectes depuis une source coherente avec des montants variables, compatibles avec une activite freelance."
                    ),
                    occurrenceCount,
                    monthlyRecurring,
                    amountStabilityScore,
                    sourceConsistencyScore,
                    recurrenceType
            );
        }

        if (occurrenceCount < MIN_OCCURRENCES) {
            return unknown(
                    "PATTERN_NOT_ENOUGH_OCCURRENCES",
                    buildPatternExplanation(
                            occurrenceCount,
                            monthlyRecurring,
                            amountStabilityScore,
                            sourceConsistencyScore,
                            patternConfidence,
                            "Nombre d'occurrences insuffisant pour etablir un revenu recurrent."
                    ),
                    occurrenceCount,
                    monthlyRecurring,
                    amountStabilityScore,
                    sourceConsistencyScore,
                    recurrenceType,
                    patternConfidence
            );
        }

        if (monthlyRecurring && amountStabilityScore >= MEDIUM_STABILITY_THRESHOLD) {
            return unknown(
                    "PATTERN_RECURRING_AMBIGUOUS",
                    buildPatternExplanation(
                            occurrenceCount,
                            true,
                            amountStabilityScore,
                            sourceConsistencyScore,
                            patternConfidence,
                            "Un revenu recurrent mensuel est detecte mais le type reste ambigu."
                    ),
                    occurrenceCount,
                    true,
                    amountStabilityScore,
                    sourceConsistencyScore,
                    MONTHLY,
                    patternConfidence
            );
        }

        return unknown(
                "PATTERN_NO_CLEAR_SIGNAL",
                buildPatternExplanation(
                        occurrenceCount,
                        monthlyRecurring,
                        amountStabilityScore,
                        sourceConsistencyScore,
                        patternConfidence,
                        "Les signaux historiques restent insuffisants ou trop irreguliers."
                ),
                occurrenceCount,
                monthlyRecurring,
                amountStabilityScore,
                sourceConsistencyScore,
                recurrenceType,
                patternConfidence
        );
    }

    private List<IncomeTransactionSnapshot> collectSimilarOccurrences(IncomeTransactionSnapshot currentTransaction,
                                                                     List<IncomeTransactionSnapshot> historicalCredits,
                                                                     String currentSource) {
        List<IncomeTransactionSnapshot> similarOccurrences = new ArrayList<>();
        if (currentTransaction != null) {
            similarOccurrences.add(currentTransaction);
        }

        if (historicalCredits == null) {
            return similarOccurrences;
        }

        for (IncomeTransactionSnapshot historicalCredit : historicalCredits) {
            if (!sameNormalizedSource(currentSource, historicalCredit)) {
                continue;
            }

            if (isDuplicateOfCurrent(currentTransaction, historicalCredit)) {
                continue;
            }

            similarOccurrences.add(historicalCredit);
        }

        return similarOccurrences;
    }

    private String normalizeKeywordSource(IncomeTransactionSnapshot transaction) {
        if (transaction == null) {
            return "";
        }

        return normalizeText(joinParts(transaction.getMerchantName(), transaction.getDescription()));
    }

    private String normalizeSource(IncomeTransactionSnapshot transaction) {
        if (transaction == null) {
            return "";
        }

        String rawSource = hasText(transaction.getMerchantName())
                ? transaction.getMerchantName()
                : transaction.getDescription();

        return normalizeSource(rawSource);
    }

    private String normalizeSource(String source) {
        return normalizeText(source);
    }

    private String normalizeText(String source) {
        if (!hasText(source)) {
            return "";
        }

        return source.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean sameNormalizedSource(String currentNormalizedSource, IncomeTransactionSnapshot historicalCredit) {
        if (!hasText(currentNormalizedSource) || historicalCredit == null) {
            return false;
        }

        return currentNormalizedSource.equals(normalizeSource(historicalCredit));
    }

    private double computeOccurrenceScore(int occurrenceCount) {
        if (occurrenceCount < MIN_OCCURRENCES) {
            return 0.20d;
        }

        return switch (occurrenceCount) {
            case 3 -> 0.65d;
            case 4 -> 0.80d;
            case 5 -> 0.90d;
            default -> 1.00d;
        };
    }

    private double computeFrequencyScore(List<IncomeTransactionSnapshot> occurrences) {
        List<LocalDate> dates = occurrences == null
                ? Collections.emptyList()
                : occurrences.stream()
                .filter(Objects::nonNull)
                .map(IncomeTransactionSnapshot::getTransactionDate)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        if (dates.size() < MIN_OCCURRENCES) {
            return 0.0d;
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
            return 0.0d;
        }

        return monthlyGaps / (double) totalGaps;
    }

    private boolean isMonthlyRecurring(double frequencyScore) {
        return frequencyScore >= 0.60d;
    }

    private double computeAmountStabilityScore(List<IncomeTransactionSnapshot> occurrences) {
        List<Double> amounts = occurrences == null
                ? Collections.emptyList()
                : occurrences.stream()
                .filter(Objects::nonNull)
                .map(IncomeTransactionSnapshot::getAmount)
                .filter(Objects::nonNull)
                .map(BigDecimal::abs)
                .map(BigDecimal::doubleValue)
                .filter(amount -> amount > 0)
                .toList();

        if (amounts.isEmpty()) {
            return 0.0d;
        }

        double mean = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
        if (mean <= 0.0d) {
            return 0.0d;
        }

        double variance = amounts.stream()
                .mapToDouble(amount -> Math.pow(amount - mean, 2))
                .average()
                .orElse(0.0d);
        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = standardDeviation / mean;

        return Math.max(0.0d, 1.0d - Math.min(coefficientOfVariation, 1.0d));
    }

    private double computeSourceConsistencyScore(String currentNormalizedSource,
                                                 List<IncomeTransactionSnapshot> similarOccurrences) {
        if (!hasText(currentNormalizedSource) || similarOccurrences == null || similarOccurrences.isEmpty()) {
            return 0.0d;
        }

        long usableOccurrences = similarOccurrences.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeSource)
                .filter(this::hasText)
                .count();

        if (usableOccurrences == 0) {
            return 0.0d;
        }

        long matchingOccurrences = similarOccurrences.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeSource)
                .filter(currentNormalizedSource::equals)
                .count();

        return matchingOccurrences / (double) usableOccurrences;
    }

    private double computePatternConfidence(double occurrenceScore,
                                            double frequencyScore,
                                            double amountStabilityScore,
                                            double sourceConsistencyScore) {
        double score = occurrenceScore * 0.35d
                + frequencyScore * 0.30d
                + amountStabilityScore * 0.20d
                + sourceConsistencyScore * 0.15d;

        return Math.max(0.0d, Math.min(score, 1.0d));
    }

    private boolean shouldClassifyAsFreelance(String currentKeywordSource,
                                              int occurrenceCount,
                                              double amountStabilityScore,
                                              double sourceConsistencyScore,
                                              double patternConfidence,
                                              boolean monthlyRecurring) {
        if (occurrenceCount < MIN_OCCURRENCES) {
            return false;
        }

        if (matchesTransferKeyword(currentKeywordSource) || matchesCashDepositKeyword(currentKeywordSource)) {
            return false;
        }

        if (containsSalaryOrRentKeyword(currentKeywordSource)) {
            return false;
        }

        if (amountStabilityScore < FREELANCE_MIN_STABILITY || amountStabilityScore > FREELANCE_MAX_STABILITY) {
            return false;
        }

        if (sourceConsistencyScore < 0.80d) {
            return false;
        }

        return patternConfidence >= 0.60d
                && (!monthlyRecurring || amountStabilityScore < HIGH_STABILITY_THRESHOLD);
    }

    private boolean matchesSalaryKeyword(String source) {
        if (!hasText(source)) {
            return false;
        }

        return containsAnyKeyword(source,
                "salaire",
                "salary",
                "payroll",
                "vir salaire",
                "virement salaire",
                "vir paie",
                "paie",
                "paie mensuelle",
                "versement salaire");
    }

    private boolean matchesTransferKeyword(String source) {
        if (!hasText(source) || containsSalaryOrRentKeyword(source) || matchesCashDepositKeyword(source)) {
            return false;
        }

        return containsAnyKeyword(source,
                "virement recu",
                "transfer recu",
                "transfert",
                "transfer",
                "virement",
                "vir");
    }

    private boolean matchesCashDepositKeyword(String source) {
        if (!hasText(source) || containsSalaryOrRentKeyword(source)) {
            return false;
        }

        return containsAnyKeyword(source,
                "cash deposit",
                "depot espece",
                "depot especes",
                "versement espece",
                "versement especes",
                "versement",
                "depot");
    }

    private boolean matchesFreelanceKeyword(String source) {
        if (!hasText(source)
                || containsSalaryOrRentKeyword(source)
                || matchesTransferKeyword(source)
                || matchesCashDepositKeyword(source)) {
            return false;
        }

        boolean strongFreelanceSignal = containsAnyKeyword(source,
                "freelance",
                "dev freelance",
                "prestation",
                "consulting",
                "mission",
                "paiement client",
                "payment client");

        if (strongFreelanceSignal) {
            return true;
        }

        boolean activitySignal = containsAnyKeyword(source,
                "design",
                "graphisme",
                "developpement",
                "development");
        boolean paymentContext = containsAnyKeyword(source,
                "paiement",
                "payment",
                "client",
                "recu",
                "reglement");

        return activitySignal && paymentContext;
    }

    private boolean containsSalaryOrRentKeyword(String source) {
        return matchesSalaryKeyword(source)
                || containsAnyKeyword(source, "loyer", "rent");
    }

    private boolean containsAnyKeyword(String source, String... keywords) {
        if (!hasText(source) || keywords == null) {
            return false;
        }

        String paddedSource = " " + source + " ";
        for (String keyword : keywords) {
            String normalizedKeyword = normalizeText(keyword);
            if (!hasText(normalizedKeyword)) {
                continue;
            }

            if (normalizedKeyword.contains(" ")) {
                if (source.contains(normalizedKeyword)) {
                    return true;
                }
                continue;
            }

            if (paddedSource.contains(" " + normalizedKeyword + " ")) {
                return true;
            }
        }

        return false;
    }

    private boolean isMostlyEndOfMonth(List<IncomeTransactionSnapshot> occurrences) {
        List<Integer> daysOfMonth = extractDaysOfMonth(occurrences);
        if (daysOfMonth.isEmpty()) {
            return false;
        }

        long matches = daysOfMonth.stream()
                .filter(day -> day >= 25 && day <= 31)
                .count();

        return matches > daysOfMonth.size() / 2.0d;
    }

    private boolean isMostlyBeginningOfMonth(List<IncomeTransactionSnapshot> occurrences) {
        List<Integer> daysOfMonth = extractDaysOfMonth(occurrences);
        if (daysOfMonth.isEmpty()) {
            return false;
        }

        long matches = daysOfMonth.stream()
                .filter(day -> day >= 1 && day <= 10)
                .count();

        return matches > daysOfMonth.size() / 2.0d;
    }

    private List<Integer> extractDaysOfMonth(List<IncomeTransactionSnapshot> occurrences) {
        return occurrences == null
                ? Collections.emptyList()
                : occurrences.stream()
                .filter(Objects::nonNull)
                .map(IncomeTransactionSnapshot::getTransactionDate)
                .filter(Objects::nonNull)
                .map(LocalDate::getDayOfMonth)
                .toList();
    }

    private String buildPatternExplanation(int occurrenceCount,
                                           boolean monthlyRecurring,
                                           double amountStabilityScore,
                                           double sourceConsistencyScore,
                                           double patternConfidence,
                                           String summary) {
        return String.format(Locale.ROOT,
                "%s Occurrences=%d, recurrence mensuelle=%s, stabilite montants=%s, coherence source=%s, confiance pattern=%s.",
                summary,
                occurrenceCount,
                monthlyRecurring ? "oui" : "non",
                formatScore(amountStabilityScore),
                formatScore(sourceConsistencyScore),
                formatScore(patternConfidence));
    }

    private String formatScore(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private IncomePatternDetectionResult unknown(String reason,
                                                 String explanation,
                                                 int occurrenceCount,
                                                 boolean monthlyRecurring,
                                                 double amountStabilityScore,
                                                 double sourceConsistencyScore,
                                                 String recurrenceType,
                                                 double confidence) {
        return result(
                IncomeTypes.UNKNOWN,
                confidence,
                reason,
                explanation,
                occurrenceCount,
                monthlyRecurring,
                amountStabilityScore,
                sourceConsistencyScore,
                recurrenceType
        );
    }

    private IncomePatternDetectionResult result(String detectedType,
                                                double confidence,
                                                String reason,
                                                String explanation,
                                                int occurrenceCount,
                                                boolean monthlyRecurring,
                                                double amountStabilityScore,
                                                double sourceConsistencyScore,
                                                String recurrenceType) {
        return new IncomePatternDetectionResult(
                detectedType,
                confidence,
                reason,
                explanation,
                occurrenceCount,
                monthlyRecurring,
                amountStabilityScore,
                sourceConsistencyScore,
                recurrenceType
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isDuplicateOfCurrent(IncomeTransactionSnapshot currentTransaction,
                                         IncomeTransactionSnapshot historicalCredit) {
        if (currentTransaction == null || historicalCredit == null) {
            return false;
        }

        return Objects.equals(normalizeSource(currentTransaction), normalizeSource(historicalCredit))
                && Objects.equals(currentTransaction.getAmount(), historicalCredit.getAmount())
                && Objects.equals(currentTransaction.getTransactionDate(), historicalCredit.getTransactionDate());
    }

    private String joinParts(String first, String second) {
        if (hasText(first) && hasText(second)) {
            return first + " " + second;
        }
        if (hasText(first)) {
            return first;
        }
        return second;
    }
}
