package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import com.adem.attijari_compass.recommendation.enums.RecommendationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RecommendationEngineServiceImpl implements RecommendationEngineService {

    private final RecommendationScoringService recommendationScoringService;

    @Override
    public List<RecommendationDto> generateRecommendations(FinancialInsightDto insight) {
        List<RecommendationDto> recommendations = new ArrayList<>();

        if (Boolean.TRUE.equals(insight.getRestaurantOverspending())) {
            recommendations.add(buildRestaurantRecommendation(insight));
        }

        if (Boolean.TRUE.equals(insight.getShoppingOverspending())) {
            recommendations.add(buildShoppingRecommendation(insight));
        }

        if (Boolean.TRUE.equals(insight.getSavingsTooLow())) {
            recommendations.add(buildSavingsRecommendation(insight));
        }

        if (Boolean.TRUE.equals(insight.getGoalDelayed())) {
            recommendations.add(buildGoalRecommendation(insight));
        }

        if (Boolean.TRUE.equals(insight.getAnomalyDetected())) {
            recommendations.add(buildAnomalyRecommendation(insight));
        }

        if (recommendations.isEmpty() && Boolean.TRUE.equals(insight.getGoodFinancialDiscipline())) {
            recommendations.add(buildPositiveRecommendation(insight));
        }

        recommendations.sort(
                Comparator.comparing(RecommendationDto::getSeverityScore, Comparator.nullsLast(Double::compareTo))
                        .reversed()
                        .thenComparing(
                                recommendation -> priorityRank(recommendation.getPriority()),
                                Comparator.reverseOrder()
                        )
        );

        return recommendations;
    }

    private RecommendationDto buildRestaurantRecommendation(FinancialInsightDto insight) {
        double gain = round(Math.max(0.0, safe(insight.getRestaurantExpense()) - safe(insight.getAverageRestaurant3Months())));
        double severityScore = recommendationScoringService.scoreRestaurantOverspending(
                safe(insight.getRestaurantExpense()),
                safe(insight.getAverageRestaurant3Months())
        );
        double overrunPercent = safe(insight.getAverageRestaurant3Months()) > 0.0
                ? ((safe(insight.getRestaurantExpense()) - safe(insight.getAverageRestaurant3Months())) / safe(insight.getAverageRestaurant3Months())) * 100.0
                : 0.0;

        return RecommendationDto.builder()
                .title("Reduire les depenses restaurant et livraison")
                .message(String.format(Locale.ROOT,
                        "Vos depenses restaurant, cafes et livraison depassent de %.1f%% votre moyenne recente. Une reduction ciblee peut liberer du budget chaque mois.",
                        Math.max(0.0, overrunPercent)))
                .suggestedAction("Fixez un plafond hebdomadaire pour les restaurants, cafes et livraisons, puis remplacez une partie des sorties par des repas prepares a la maison.")
                .type(RecommendationType.BUDGET_ALERT)
                .priority(RecommendationPriority.HIGH)
                .category("RESTAURANT")
                .sourceType(RecommendationSourceType.EXPENSE.name())
                .estimatedMonthlyGain(gain)
                .estimatedGoalImpactMonths(null)
                .confidenceScore(confidenceFromGap(overrunPercent, 78.0, 94.0))
                .severityScore(severityScore)
                .explanation(String.format(Locale.ROOT,
                        "Le mois analyse atteint %s contre une moyenne de %s sur les 3 derniers mois.",
                        formatAmount(insight.getRestaurantExpense()),
                        formatAmount(insight.getAverageRestaurant3Months())))
                .basedOn(List.of(
                        "Depenses restaurant, cafes et livraison du mois: " + formatAmount(insight.getRestaurantExpense()),
                        "Moyenne restaurant, cafes et livraison sur 3 mois: " + formatAmount(insight.getAverageRestaurant3Months()),
                        "Seuil de declenchement: 120% de la moyenne recente"
                ))
                .actionable(true)
                .build();
    }

    private RecommendationDto buildShoppingRecommendation(FinancialInsightDto insight) {
        double gain = round(Math.max(0.0, safe(insight.getShoppingExpense()) - safe(insight.getAverageShopping3Months())));
        double severityScore = recommendationScoringService.scoreShoppingOverspending(
                safe(insight.getShoppingExpense()),
                safe(insight.getAverageShopping3Months())
        );
        double overrunPercent = safe(insight.getAverageShopping3Months()) > 0.0
                ? ((safe(insight.getShoppingExpense()) - safe(insight.getAverageShopping3Months())) / safe(insight.getAverageShopping3Months())) * 100.0
                : 0.0;

        return RecommendationDto.builder()
                .title("Maitriser les depenses shopping")
                .message(String.format(Locale.ROOT,
                        "Vos achats shopping sont superieurs de %.1f%% a votre rythme habituel. Une meilleure priorisation peut reduire cette derive.",
                        Math.max(0.0, overrunPercent)))
                .suggestedAction("Differez les achats non essentiels 48 heures et fixez une enveloppe shopping mensuelle avant toute nouvelle depense.")
                .type(RecommendationType.HABIT_IMPROVEMENT)
                .priority(RecommendationPriority.MEDIUM)
                .category("SHOPPING")
                .sourceType(RecommendationSourceType.EXPENSE.name())
                .estimatedMonthlyGain(gain)
                .estimatedGoalImpactMonths(null)
                .confidenceScore(confidenceFromGap(overrunPercent, 72.0, 90.0))
                .severityScore(severityScore)
                .explanation(String.format(Locale.ROOT,
                        "Le niveau shopping du mois est de %s contre une moyenne recente de %s.",
                        formatAmount(insight.getShoppingExpense()),
                        formatAmount(insight.getAverageShopping3Months())))
                .basedOn(List.of(
                        "Depenses shopping du mois: " + formatAmount(insight.getShoppingExpense()),
                        "Moyenne shopping sur 3 mois: " + formatAmount(insight.getAverageShopping3Months()),
                        "Seuil de declenchement: 115% de la moyenne recente"
                ))
                .actionable(true)
                .build();
    }

    private RecommendationDto buildSavingsRecommendation(FinancialInsightDto insight) {
        double income = safe(insight.getTotalIncome());
        double savingsGap = income > 0.0
                ? Math.max(0.0, (income * 0.10) - safe(insight.getSavingsAmount()))
                : 0.0;
        double severityScore = recommendationScoringService.scoreSavingsRate(safe(insight.getSavingsRate()));

        return RecommendationDto.builder()
                .title("Renforcer votre epargne")
                .message(String.format(Locale.ROOT,
                        "Votre taux d'epargne actuel est de %.1f%%, sous le seuil de vigilance de 10%%. Votre marge de securite reste trop faible.",
                        safe(insight.getSavingsRate())))
                .suggestedAction("Automatisez une mise de cote des reception du revenu et priorisez les reductions sur les depenses variables les plus compressibles.")
                .type(RecommendationType.SAVING_OPPORTUNITY)
                .priority(RecommendationPriority.HIGH)
                .category("AUTRES")
                .sourceType(RecommendationSourceType.EXPENSE.name())
                .estimatedMonthlyGain(round(savingsGap))
                .estimatedGoalImpactMonths(null)
                .confidenceScore(confidenceFromGap(10.0 - safe(insight.getSavingsRate()), 80.0, 95.0))
                .severityScore(severityScore)
                .explanation(String.format(Locale.ROOT,
                        "Avec %s de revenus et %s de depenses, votre solde net mensuel reste a %s.",
                        formatAmount(insight.getTotalIncome()),
                        formatAmount(insight.getTotalExpenses()),
                        formatAmount(insight.getSavingsAmount())))
                .basedOn(List.of(
                        "Taux d'epargne actuel: " + formatPercent(insight.getSavingsRate()),
                        "Seuil minimal recommande: 10.00%",
                        "Potentiel d'economies identifie: " + formatAmount(insight.getPossibleSavingsPotential())
                ))
                .actionable(true)
                .build();
    }

    private RecommendationDto buildGoalRecommendation(FinancialInsightDto insight) {
        double severityScore = recommendationScoringService.scoreGoalAcceleration(
                safe(insight.getCurrentMonthlyContribution()),
                safe(insight.getRequiredMonthlyContributionForGoal())
        );
        double monthlyGap = Math.max(0.0,
                safe(insight.getRequiredMonthlyContributionForGoal()) - safe(insight.getCurrentMonthlyContribution()));
        double estimatedDelayMonths = estimateGoalDelayMonths(
                safe(insight.getCurrentMonthlyContribution()),
                safe(insight.getRequiredMonthlyContributionForGoal())
        );

        return RecommendationDto.builder()
                .title("Accelerer votre objectif")
                .message("Votre contribution mensuelle actuelle semble insuffisante pour atteindre votre objectif dans les delais prevus.")
                .suggestedAction("Augmentez le versement mensuel consacre a l'objectif ou reaffectez en priorite une partie du potentiel d'economies detecte.")
                .type(RecommendationType.GOAL_ACCELERATION)
                .priority(RecommendationPriority.CRITICAL)
                .category("OBJECTIF")
                .sourceType(RecommendationSourceType.GOAL.name())
                .estimatedMonthlyGain(round(monthlyGap))
                .estimatedGoalImpactMonths(estimatedDelayMonths)
                .confidenceScore(confidenceFromGap(monthlyGap, 74.0, 92.0))
                .severityScore(severityScore)
                .explanation(String.format(Locale.ROOT,
                        "La contribution estimee est de %s par mois alors que %s sont necessaires pour rester alignes avec l'echeance.",
                        formatAmount(insight.getCurrentMonthlyContribution()),
                        formatAmount(insight.getRequiredMonthlyContributionForGoal())))
                .basedOn(List.of(
                        "Contribution mensuelle actuelle estimee: " + formatAmount(insight.getCurrentMonthlyContribution()),
                        "Contribution mensuelle requise: " + formatAmount(insight.getRequiredMonthlyContributionForGoal()),
                        "Impact delai estime: " + formatAmount(estimatedDelayMonths) + " mois"
                ))
                .actionable(true)
                .build();
    }

    private RecommendationDto buildAnomalyRecommendation(FinancialInsightDto insight) {
        double severityScore = recommendationScoringService.scoreAnomaly(
                safe(insight.getAnomalyAmount()),
                averageReferenceForAnomaly(insight)
        );

        return RecommendationDto.builder()
                .title("Verifier une depense inhabituelle")
                .message("Une depense inhabituelle a ete detectee dans vos operations recentes. Une verification rapide est recommandee.")
                .suggestedAction("Controlez la transaction concernee, confirmez qu'elle est legitime et recategorisez-la si le classement ne correspond pas a la realite.")
                .type(RecommendationType.ANOMALY_DETECTION)
                .priority(RecommendationPriority.MEDIUM)
                .category("ANOMALIE")
                .sourceType(RecommendationSourceType.EXPENSE.name())
                .estimatedMonthlyGain(null)
                .estimatedGoalImpactMonths(null)
                .confidenceScore(confidenceFromGap(safe(insight.getAnomalyAmount()), 68.0, 88.0))
                .severityScore(severityScore)
                .explanation(String.format(Locale.ROOT,
                        "Le montant anormal detecte atteint %s, nettement au-dessus de votre comportement de depense recent.",
                        formatAmount(insight.getAnomalyAmount())))
                .basedOn(List.of(
                        "Montant de l'anomalie detectee: " + formatAmount(insight.getAnomalyAmount()),
                        "Analyse basee sur les 3 derniers mois de depenses",
                        "Verification manuelle recommandee si l'operation est inattendue"
                ))
                .actionable(true)
                .build();
    }

    private RecommendationDto buildPositiveRecommendation(FinancialInsightDto insight) {
        double severityScore = recommendationScoringService.scorePositiveFeedback(
                safe(insight.getSavingsRate()),
                safe(insight.getPossibleSavingsPotential())
        );

        return RecommendationDto.builder()
                .title("Bonne gestion financiere")
                .message("Vos indicateurs recents montrent une gestion saine, sans signal budgetaire ou anomalie majeure.")
                .suggestedAction("Maintenez cette discipline et envisagez d'augmenter progressivement votre epargne ou le financement de vos objectifs.")
                .type(RecommendationType.POSITIVE_FEEDBACK)
                .priority(RecommendationPriority.LOW)
                .category("GENERAL")
                .sourceType(RecommendationSourceType.EXPENSE.name())
                .estimatedMonthlyGain(null)
                .estimatedGoalImpactMonths(null)
                .confidenceScore(82.0)
                .severityScore(severityScore)
                .explanation(String.format(Locale.ROOT,
                        "Votre taux d'epargne est de %s et aucun depassement significatif n'a ete releve sur les postes critiques.",
                        formatPercent(insight.getSavingsRate())))
                .basedOn(List.of(
                        "Taux d'epargne: " + formatPercent(insight.getSavingsRate()),
                        "Potentiel d'economies residuel: " + formatAmount(insight.getPossibleSavingsPotential()),
                        "Aucune anomalie recente detectee"
                ))
                .actionable(true)
                .build();
    }

    private Double priorityRank(RecommendationPriority priority) {
        return switch (priority) {
            case CRITICAL -> 4.0;
            case HIGH -> 3.0;
            case MEDIUM -> 2.0;
            case LOW -> 1.0;
            case null -> 0.0;
        };
    }

    private double confidenceFromGap(double rawGap, double min, double max) {
        double normalizedGap = Math.min(1.0, Math.max(0.0, rawGap / 100.0));
        return round(min + ((max - min) * normalizedGap));
    }

    private double estimateGoalDelayMonths(double currentContribution, double requiredContribution) {
        if (requiredContribution <= 0.0) {
            return 0.0;
        }
        if (currentContribution <= 0.0) {
            return 12.0;
        }
        double delayFactor = Math.max(0.0, (requiredContribution / currentContribution) - 1.0);
        return round(Math.min(12.0, Math.max(1.0, delayFactor * 6.0)));
    }

    private double averageReferenceForAnomaly(FinancialInsightDto insight) {
        double reference = Math.max(
                safe(insight.getAverageRestaurant3Months()),
                safe(insight.getAverageShopping3Months())
        );
        return reference > 0.0 ? reference : Math.max(1.0, safe(insight.getTransportExpense()));
    }

    private String formatAmount(Double amount) {
        return String.format(Locale.ROOT, "%.2f", safe(amount));
    }

    private String formatPercent(Double value) {
        return String.format(Locale.ROOT, "%.2f%%", safe(value));
    }

    private double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
