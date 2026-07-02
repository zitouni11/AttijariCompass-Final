package com.adem.attijari_compass.service.admin;

import com.adem.attijari_compass.dto.admin.decision.TransactionSourceActionPlanItemDto;
import com.adem.attijari_compass.dto.admin.decision.TransactionSourceAnalysisDto;
import com.adem.attijari_compass.dto.admin.decision.TransactionSourceDecisionDto;
import com.adem.attijari_compass.dto.admin.decision.TransactionSourceDiagnosticDto;
import com.adem.attijari_compass.dto.admin.decision.TransactionSourceImpactDto;
import com.adem.attijari_compass.dto.admin.decision.TransactionSourceMetricDto;
import com.adem.attijari_compass.dto.admin.decision.TransactionSourceReasoningDto;
import com.adem.attijari_compass.dto.admin.decision.TransactionSourceStrategicDecisionDto;
import com.adem.attijari_compass.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTransactionSourceDecisionService {

    private static final double DIGITALISATION_TARGET = 85.0d;
    private static final String NO_DATA_MESSAGE =
            "Aucune transaction disponible pour générer l'analyse décisionnelle.";
    private static final String ERROR_MESSAGE =
            "Impossible de générer l'analyse décisionnelle des sources de transactions pour le moment.";

    private final TransactionRepository transactionRepository;

    public TransactionSourceDecisionDto analyzeTransactionSources() {
        try {
            List<Object[]> rawMetrics = transactionRepository.findPowerBiTransactionSourceMetrics();
            log.info("Admin decision transaction source metrics loaded from payment_method scope: {}", rawMetrics.size());
            return buildAnalysisResponse(rawMetrics);
        } catch (RuntimeException ex) {
            log.error("Unable to generate admin transaction source decision analysis", ex);
            return buildFallbackResponse(ERROR_MESSAGE);
        }
    }

    private TransactionSourceDecisionDto buildAnalysisResponse(List<Object[]> rawMetrics) {
        Map<DecisionSource, AggregatedSourceMetric> metrics = initializeMetrics();

        for (Object[] rawMetric : rawMetrics) {
            DecisionSource source = mapDecisionSource(readString(rawMetric, 0));
            long count = readLong(rawMetric, 1);
            double amount = readDouble(rawMetric, 2);
            metrics.get(source).add(count, amount);
            log.info("Admin decision source metric: source={}, transactions={}, totalAmount={}",
                    source, count, round(amount));
        }

        long total = metrics.values().stream().mapToLong(AggregatedSourceMetric::transactionCount).sum();
        if (total == 0) {
            return buildFallbackResponse(NO_DATA_MESSAGE);
        }

        long card = metrics.get(DecisionSource.CARD).transactionCount();
        long cash = metrics.get(DecisionSource.CASH).transactionCount();
        long bankTransfer = metrics.get(DecisionSource.BANK_TRANSFER).transactionCount();
        long digital = card + bankTransfer;
        double rate = percent(digital, total);
        double gap = round(rate - DIGITALISATION_TARGET);
        String status = resolveStatus(rate);
        String priority = resolvePriority(rate);
        DecisionSource dominant = resolveDominantSource(metrics);
        long digitalNeeded = (long) Math.ceil((DIGITALISATION_TARGET / 100.0d) * total);
        long transactionsToConvert = Math.max(0, digitalNeeded - digital);

        log.info("Admin decision summary: total={}, card={}, cash={}, bankTransfer={}, digital={}, rate={}, gap={}, dominant={}, priority={}, toConvert={}",
                total, card, cash, bankTransfer, digital, rate, gap, dominant, priority, transactionsToConvert);

        return new TransactionSourceDecisionDto(
                total,
                buildMetricDtos(metrics, total),
                dominant.name(),
                rate,
                DIGITALISATION_TARGET,
                gap,
                priority,
                digital,
                buildDiagnostic(status, rate, gap),
                buildAnalysis(total, card, cash, bankTransfer, digital, rate, gap, status),
                buildReasoning(card, cash, bankTransfer, total, digital, gap, priority),
                buildImpact(rate, transactionsToConvert),
                buildStrategicDecision(total, card, cash, bankTransfer, digital, rate, gap, transactionsToConvert, digitalNeeded),
                buildActionPlan(card, cash, bankTransfer, total, gap, transactionsToConvert),
                buildExecutiveConclusion(rate, gap, transactionsToConvert),
                buildRecommendedActions(rate),
                null
        );
    }

    private TransactionSourceDecisionDto buildFallbackResponse(String message) {
        Map<DecisionSource, AggregatedSourceMetric> metrics = initializeMetrics();
        return new TransactionSourceDecisionDto(
                0,
                buildMetricDtos(metrics, 0),
                null,
                0,
                DIGITALISATION_TARGET,
                -DIGITALISATION_TARGET,
                "HIGH",
                0,
                new TransactionSourceDiagnosticDto("Critique", message),
                new TransactionSourceAnalysisDto(
                        message,
                        "Aucune tendance ne peut être calculée sans transactions qualifiées.",
                        "Le pilotage de la digitalisation est impossible tant que les données sont indisponibles.",
                        "Vérifier l'alimentation des transactions puis relancer l'analyse."
                ),
                new TransactionSourceReasoningDto(0, 0, 0, 0, 0, -DIGITALISATION_TARGET,
                        "Aucune règle décisionnelle n'a été appliquée car le périmètre ne contient aucune transaction."),
                new TransactionSourceImpactDto(0, DIGITALISATION_TARGET, DIGITALISATION_TARGET,
                        "Alimenter la base avec des transactions qualifiées.",
                        "Estimation métier indisponible sans données."),
                new TransactionSourceStrategicDecisionDto(
                        "Atteindre 85 % de transactions digitales.",
                        "Collecter des transactions qualifiées avant de prioriser un levier.",
                        "Fiabiliser la donnée source.",
                        "Aucun calcul fiable ne peut être produit sans données transactionnelles.",
                        "Relancer l'analyse après correction de la donnée.",
                        0,
                        0,
                        "Impact non estimable sans données."
                ),
                List.of(),
                message,
                List.of("Vérifier la collecte des transactions.", "Relancer l'analyse après correction des données."),
                message
        );
    }

    private Map<DecisionSource, AggregatedSourceMetric> initializeMetrics() {
        Map<DecisionSource, AggregatedSourceMetric> metrics = new EnumMap<>(DecisionSource.class);
        for (DecisionSource source : DecisionSource.values()) {
            metrics.put(source, new AggregatedSourceMetric());
        }
        return metrics;
    }

    private List<TransactionSourceMetricDto> buildMetricDtos(Map<DecisionSource, AggregatedSourceMetric> groupedMetrics, long total) {
        List<TransactionSourceMetricDto> result = new ArrayList<>();
        for (DecisionSource source : DecisionSource.values()) {
            AggregatedSourceMetric metric = groupedMetrics.get(source);
            result.add(new TransactionSourceMetricDto(
                    source.name(),
                    metric.transactionCount(),
                    percent(metric.transactionCount(), total),
                    round(metric.totalAmount()),
                    round(metric.averageAmount())
            ));
        }
        return result;
    }

    private String resolveStatus(double rate) {
        if (rate >= DIGITALISATION_TARGET) {
            return "Objectif atteint";
        }
        if (rate >= 80.0d) {
            return "Presque atteint";
        }
        if (rate >= 60.0d) {
            return "Attention";
        }
        return "Critique";
    }

    private String resolvePriority(double rate) {
        if (rate >= DIGITALISATION_TARGET) {
            return "LOW";
        }
        if (rate >= 60.0d) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private DecisionSource resolveDominantSource(Map<DecisionSource, AggregatedSourceMetric> metrics) {
        DecisionSource dominantSource = DecisionSource.CASH;
        long dominantCount = -1;
        for (DecisionSource source : List.of(DecisionSource.CARD, DecisionSource.CASH, DecisionSource.BANK_TRANSFER)) {
            long count = metrics.get(source).transactionCount();
            if (count > dominantCount) {
                dominantSource = source;
                dominantCount = count;
            }
        }
        if (dominantCount <= 0 && metrics.get(DecisionSource.UNKNOWN).transactionCount() > 0) {
            return DecisionSource.UNKNOWN;
        }
        return dominantSource;
    }

    private TransactionSourceDiagnosticDto buildDiagnostic(String status, double rate, double gap) {
        return new TransactionSourceDiagnosticDto(
                status,
                String.format("Le niveau de digitalisation est de %.2f %% pour un objectif de %.0f %%, soit un écart de %.2f %%.", rate, DIGITALISATION_TARGET, gap)
        );
    }

    private TransactionSourceAnalysisDto buildAnalysis(
            long total,
            long card,
            long cash,
            long bankTransfer,
            long digital,
            double rate,
            double gap,
            String status
    ) {
        String constat = String.format(
                "Sur %d transactions, %d sont digitales (%d cartes et %d virements) et %d restent en cash.",
                total, digital, card, bankTransfer, cash
        );
        String interpretation = String.format(
                "La digitalisation atteint %.2f %% : l'état décisionnel est \"%s\".",
                rate, status
        );
        String risque = gap >= 0
                ? String.format("L'objectif de %.0f %% est atteint ; le risque principal est de ne pas maintenir ce niveau dans le temps.", DIGITALISATION_TARGET)
                : String.format("L'écart de %.2f %% montre que le cash restant empêche encore d'atteindre l'objectif de %.0f %%.", gap, DIGITALISATION_TARGET);
        return new TransactionSourceAnalysisDto(
                constat,
                interpretation,
                risque,
                decisionForRate(rate)
        );
    }

    private String decisionForRate(double rate) {
        if (rate >= DIGITALISATION_TARGET) {
            return "Maintenir les efforts et suivre l'évolution.";
        }
        if (rate >= 80.0d) {
            return "Réduire légèrement le cash restant et encourager les virements.";
        }
        if (rate >= 60.0d) {
            return "Mettre en place des actions ciblées vers les paiements digitaux.";
        }
        return "Lancer une campagne forte de digitalisation.";
    }

    private TransactionSourceReasoningDto buildReasoning(
            long card,
            long cash,
            long bankTransfer,
            long total,
            long digital,
            double gap,
            String priority
    ) {
        return new TransactionSourceReasoningDto(
                card,
                cash,
                bankTransfer,
                total,
                digital,
                gap,
                String.format("La priorité est %s car CARD + BANK_TRANSFER = %d transactions sur %d, avec un écart de %.2f %% par rapport à l'objectif de %.0f %% et %d transactions cash restantes.",
                        priority, digital, total, gap, DIGITALISATION_TARGET, cash)
        );
    }

    private TransactionSourceImpactDto buildImpact(double rate, long transactionsToConvert) {
        return new TransactionSourceImpactDto(
                rate,
                DIGITALISATION_TARGET,
                rate >= DIGITALISATION_TARGET ? 0 : round(DIGITALISATION_TARGET - rate),
                transactionsToConvert > 0
                        ? String.format("Convertir environ %d transactions cash vers carte ou virement.", transactionsToConvert)
                        : "Maintenir la part actuelle des transactions digitales.",
                "Projection simple basée sur des règles métier, sans modèle de machine learning."
        );
    }

    private TransactionSourceStrategicDecisionDto buildStrategicDecision(
            long total,
            long card,
            long cash,
            long bankTransfer,
            long digital,
            double rate,
            double gap,
            long transactionsToConvert,
            long digitalNeeded
    ) {
        String mainLever = cash > 0 && rate < DIGITALISATION_TARGET
                ? "Convertir une partie du cash vers CARD ou BANK_TRANSFER."
                : "Maintenir le niveau de digitalisation déjà atteint.";
        String secondaryLever = percent(bankTransfer, total) < 25.0d
                ? "Encourager les virements bancaires."
                : "Suivre les virements sans action corrective prioritaire.";
        String justification = String.format(
                "Les cartes représentent %.2f %% des transactions, les virements %.2f %% et le cash %.2f %%. L'objectif de %.0f %% est %s avec un écart de %.2f %%.",
                percent(card, total),
                percent(bankTransfer, total),
                percent(cash, total),
                DIGITALISATION_TARGET,
                rate >= DIGITALISATION_TARGET ? "atteint" : "presque atteint",
                gap
        );
        return new TransactionSourceStrategicDecisionDto(
                "Atteindre 85 % de transactions digitales.",
                mainLever,
                secondaryLever,
                justification,
                decisionForRate(rate),
                digitalNeeded,
                transactionsToConvert,
                transactionsToConvert > 0
                        ? String.format("Il suffit de convertir environ %d transactions cash vers carte ou virement pour atteindre l'objectif de %.0f %%.", transactionsToConvert, DIGITALISATION_TARGET)
                        : String.format("Aucune conversion supplémentaire n'est nécessaire : %d transactions digitales couvrent l'objectif.", digital)
        );
    }

    private List<TransactionSourceActionPlanItemDto> buildActionPlan(
            long card,
            long cash,
            long bankTransfer,
            long total,
            double gap,
            long transactionsToConvert
    ) {
        return List.of(
                new TransactionSourceActionPlanItemDto(
                        transactionsToConvert > 0 ? "Haute" : "Faible",
                        "Convertir une partie du cash vers les paiements digitaux",
                        String.format("Cash = %d transactions, écart objectif = %.2f %%", cash, gap),
                        transactionsToConvert > 0
                                ? String.format("+%d transactions digitales suffisent pour atteindre 85 %%", transactionsToConvert)
                                : "Objectif déjà atteint",
                        "Faible"
                ),
                new TransactionSourceActionPlanItemDto(
                        "Moyenne",
                        "Encourager les virements bancaires",
                        String.format("BANK_TRANSFER représente %.2f %% des transactions", percent(bankTransfer, total)),
                        "Amélioration progressive de la digitalisation",
                        "Moyenne"
                ),
                new TransactionSourceActionPlanItemDto(
                        "Faible",
                        "Maintenir les avantages liés aux cartes",
                        String.format("CARD est déjà dominant avec %.2f %% des transactions", percent(card, total)),
                        "Stabiliser l'usage des cartes",
                        "Faible"
                )
        );
    }

    private String buildExecutiveConclusion(double rate, double gap, long transactionsToConvert) {
        return String.format(
                "Les indicateurs montrent que la plateforme atteint un niveau de digitalisation de %.2f %%, soit un écart de %.2f %% par rapport à l'objectif fixé à %.0f %%. La priorité n'est pas d'augmenter massivement l'usage des cartes, déjà majoritaires, mais de convertir une petite partie des transactions cash vers des moyens digitaux. Environ %d transactions converties suffiraient à atteindre l'objectif.",
                rate,
                Math.abs(gap),
                DIGITALISATION_TARGET,
                transactionsToConvert
        );
    }

    private List<String> buildRecommendedActions(double rate) {
        if (rate >= DIGITALISATION_TARGET) {
            return List.of(
                    "Maintenir les efforts et suivre l'évolution mensuelle.",
                    "Surveiller la stabilité des transactions digitales.",
                    "Préserver les avantages liés aux cartes et aux virements."
            );
        }
        if (rate >= 80.0d) {
            return List.of(
                    "Réduire légèrement le cash restant.",
                    "Encourager les virements bancaires.",
                    "Maintenir les avantages liés aux cartes.",
                    "Suivre l'évolution mensuelle de la digitalisation."
            );
        }
        if (rate >= 60.0d) {
            return List.of(
                    "Cibler les segments qui utilisent encore le cash.",
                    "Promouvoir les paiements par carte et virement.",
                    "Mesurer l'évolution après chaque campagne."
            );
        }
        return List.of(
                "Lancer une campagne forte de digitalisation.",
                "Identifier les usages cash prioritaires.",
                "Mettre en place des incitations digitales."
        );
    }

    private DecisionSource mapDecisionSource(String source) {
        if (source == null || source.isBlank()) {
            return DecisionSource.UNKNOWN;
        }
        try {
            String normalized = source.trim().toUpperCase();
            if ("DIGITAL_WALLET".equals(normalized)) {
                return DecisionSource.CARD;
            }
            return DecisionSource.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return DecisionSource.UNKNOWN;
        }
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return round((value * 100.0d) / total);
    }

    private String readString(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return null;
        }
        return row[index].toString();
    }

    private long readLong(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0;
        }
        Object value = row[index];
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private double readDouble(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0;
        }
        Object value = row[index];
        if (value instanceof BigDecimal decimal) {
            return decimal.doubleValue();
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private enum DecisionSource {
        CARD,
        CASH,
        BANK_TRANSFER,
        UNKNOWN
    }

    private static final class AggregatedSourceMetric {
        private long transactionCount;
        private double totalAmount;

        void add(long count, double amount) {
            this.transactionCount += count;
            this.totalAmount += amount;
        }

        long transactionCount() {
            return transactionCount;
        }

        double totalAmount() {
            return totalAmount;
        }

        double averageAmount() {
            if (transactionCount == 0) {
                return 0;
            }
            return totalAmount / transactionCount;
        }
    }
}
