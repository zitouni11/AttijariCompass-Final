package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.FinancialScoreBreakdownDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationSummaryDto;
import com.adem.attijari_compass.recommendation.enums.CurrentMonthSeverity;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendationNarrativeServiceImpl implements RecommendationNarrativeService {

    @Override
    public RecommendationSummaryDto buildSummary(
            FinancialInsightDto insight,
            FinancialScoreBreakdownDto scoreBreakdown,
            List<RecommendationDto> recommendations
    ) {
        Map<RecommendationPriority, Integer> counts = new EnumMap<>(RecommendationPriority.class);
        for (RecommendationPriority priority : RecommendationPriority.values()) {
            counts.put(priority, 0);
        }

        recommendations.stream()
                .map(RecommendationDto::getPriority)
                .forEach(priority -> {
                    if (priority != null) {
                        counts.put(priority, counts.get(priority) + 1);
                    }
                });

        double totalEstimatedMonthlyGain = recommendations.stream()
                .map(RecommendationDto::getEstimatedMonthlyGain)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        double currentMonthIncome = safe(insight != null ? insight.getTotalIncome() : null);
        double currentMonthExpenses = safe(insight != null ? insight.getTotalExpenses() : null);
        double currentMonthNetBalance = insight != null && insight.getRemainingBalance() != null
                ? safe(insight.getRemainingBalance())
                : round(currentMonthIncome - currentMonthExpenses);
        double currentMonthSavingsRate = safe(insight != null ? insight.getSavingsRate() : null);
        CurrentMonthSeverity currentMonthSeverity = scoreBreakdown != null && scoreBreakdown.getCurrentMonthSeverity() != null
                ? scoreBreakdown.getCurrentMonthSeverity()
                : CurrentMonthSeverity.NORMAL;

        RecommendationSummaryDto summary = RecommendationSummaryDto.builder()
                .totalRecommendations(recommendations.size())
                .criticalCount(counts.get(RecommendationPriority.CRITICAL))
                .highCount(counts.get(RecommendationPriority.HIGH))
                .mediumCount(counts.get(RecommendationPriority.MEDIUM))
                .lowCount(counts.get(RecommendationPriority.LOW))
                .totalEstimatedMonthlyGain(round(totalEstimatedMonthlyGain))
                .financialScore(scoreBreakdown != null ? scoreBreakdown.getFinalScore() : null)
                .financialScoreLabel(scoreBreakdown != null ? scoreBreakdown.getLabel() : null)
                .financialScoreBreakdown(scoreBreakdown)
                .currentMonthSeverity(currentMonthSeverity)
                .currentMonthStatusLabel(currentMonthSeverity == CurrentMonthSeverity.CRITICAL ? "Critique" : "Normale")
                .currentMonthIncome(round(currentMonthIncome))
                .currentMonthExpenses(round(currentMonthExpenses))
                .currentMonthNetBalance(round(currentMonthNetBalance))
                .currentMonthSavingsRate(round(currentMonthSavingsRate))
                .build();

        summary.setGlobalStatus(resolveGlobalStatus(insight, counts, summary));
        summary.setAiSummary(buildAiSummary(summary, recommendations, counts));
        return summary;
    }

    private String resolveGlobalStatus(
            FinancialInsightDto insight,
            Map<RecommendationPriority, Integer> counts,
            RecommendationSummaryDto summary
    ) {
        Integer score = summary.getFinancialScore();

        if (summary.getCurrentMonthSeverity() == CurrentMonthSeverity.CRITICAL
                || counts.get(RecommendationPriority.CRITICAL) > 0
                || (score != null && score <= 34)) {
            return "CRITIQUE";
        }
        if (counts.get(RecommendationPriority.HIGH) > 0
                || counts.get(RecommendationPriority.MEDIUM) > 1
                || Boolean.TRUE.equals(insight != null ? insight.getAnomalyDetected() : null)
                || (score != null && score <= 64)) {
            return "A SURVEILLER";
        }
        return "STABLE";
    }

    private String buildAiSummary(
            RecommendationSummaryDto summary,
            List<RecommendationDto> recommendations,
            Map<RecommendationPriority, Integer> counts
    ) {
        Integer score = summary.getFinancialScore();
        String scoreLabel = summary.getFinancialScoreLabel();
        String scoreText = score != null ? score + "/100" : "indisponible";
        String scoreLabelText = scoreLabel != null ? scoreLabel : "Indisponible";
        double totalEstimatedMonthlyGain = summary.getTotalEstimatedMonthlyGain() != null
                ? summary.getTotalEstimatedMonthlyGain()
                : 0.0d;
        String globalStatus = summary.getGlobalStatus();
        String currentMonthSnapshot = String.format(
                Locale.ROOT,
                "revenus %.2f, depenses %.2f, solde net %.2f, epargne %.2f%%",
                safe(summary.getCurrentMonthIncome()),
                safe(summary.getCurrentMonthExpenses()),
                safe(summary.getCurrentMonthNetBalance()),
                safe(summary.getCurrentMonthSavingsRate())
        );

        if (summary.getCurrentMonthSeverity() == CurrentMonthSeverity.CRITICAL) {
            return String.format(
                    Locale.ROOT,
                    "Le mois courant est critique avec %s. Votre score financier est de %s (%s). La priorite immediate est de retablir l'equilibre avant toute optimisation secondaire.",
                    currentMonthSnapshot,
                    scoreText,
                    scoreLabelText
            );
        }

        if (recommendations.isEmpty()) {
            return String.format(Locale.ROOT,
                    "Votre score financier est de %s (%s). Aucun signal prioritaire n'a ete detecte pour le moment, mais l'analyse gagnera en fiabilite avec davantage d'historique.",
                    scoreText,
                    scoreLabelText);
        }

        String topThemes = recommendations.stream()
                .limit(2)
                .map(RecommendationDto::getTitle)
                .collect(Collectors.joining(", "));

        return switch (globalStatus) {
            case "CRITIQUE" -> String.format(Locale.ROOT,
                    "Votre score financier est de %s (%s). La situation demande une action rapide. %d recommandation(s) dont %d critique(s) ont ete generees. Les sujets prioritaires sont: %s. Potentiel mensuel estime: %.2f.",
                    scoreText,
                    scoreLabelText,
                    recommendations.size(),
                    counts.get(RecommendationPriority.CRITICAL),
                    topThemes,
                    round(totalEstimatedMonthlyGain));
            case "A SURVEILLER" -> String.format(Locale.ROOT,
                    "Votre score financier est de %s (%s). Plusieurs signaux appellent une vigilance renforcee. %d recommandation(s) ont ete generees, avec %d priorite(s) haute(s). Les principaux leviers concernent: %s. Potentiel mensuel estime: %.2f.",
                    scoreText,
                    scoreLabelText,
                    recommendations.size(),
                    counts.get(RecommendationPriority.HIGH),
                    topThemes,
                    round(totalEstimatedMonthlyGain));
            default -> String.format(Locale.ROOT,
                    "Votre score financier est de %s (%s). Votre profil reste globalement stable. %d recommandation(s) legere(s) ou positive(s) ont ete formulees. Points marquants: %s.",
                    scoreText,
                    scoreLabelText,
                    recommendations.size(),
                    topThemes);
        };
    }

    private double safe(Double value) {
        return value != null ? value : 0.0d;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
