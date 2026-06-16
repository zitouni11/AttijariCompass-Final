package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomeInsightResponse;
import com.adem.attijari_compass.dto.income.IncomeRecommendation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class IncomeRecommendationServiceImpl implements IncomeRecommendationService {

    private static final String HIGH = "HIGH";
    private static final String MEDIUM = "MEDIUM";
    private static final String LOW = "LOW";
    private static final String STABILITY = "STABILITY";
    private static final String SAVING = "SAVING";
    private static final String RISK = "RISK";
    private static final String OPTIMIZATION = "OPTIMIZATION";
    private static final int MAX_RECOMMENDATIONS = 3;

    @Override
    public List<IncomeRecommendation> generateRecommendations(IncomeInsightResponse insight) {
        Map<String, IncomeRecommendation> recommendations = new LinkedHashMap<>();

        if (insight == null) {
            addRecommendation(recommendations, recommendation(
                    "Consolider l'analyse de revenus",
                    "Les donnees disponibles sont encore insuffisantes pour produire des recommandations personnalisees. Continuez a consolider votre historique de revenus.",
                    LOW,
                    STABILITY
            ));
            return limitRecommendations(recommendations);
        }

        String primaryIncomeType = IncomeTypes.normalize(insight.getPrimaryIncomeType());
        String incomeStability = normalizeLabel(insight.getIncomeStability());
        boolean hasSecondaryIncome = Boolean.TRUE.equals(insight.getHasSecondaryIncome());

        addPrimaryCriticalRecommendation(recommendations, primaryIncomeType);
        addStabilityCriticalRecommendation(recommendations, incomeStability);
        if (hasSecondaryIncome) {
            addRecommendation(recommendations, recommendation(
                    "Optimiser vos revenus multiples",
                    "Vous semblez disposer de plusieurs sources de revenus. Les suivre separement peut ameliorer votre budget, votre epargne et vos objectifs.",
                    MEDIUM,
                    OPTIMIZATION
            ));
        }
        addPrimaryComplementaryRecommendation(recommendations, primaryIncomeType);
        addStabilityComplementaryRecommendation(recommendations, incomeStability);

        if (recommendations.isEmpty()) {
            addRecommendation(recommendations, recommendation(
                    "Consolider la lecture de vos revenus",
                    buildDefaultDescription(insight),
                    LOW,
                    STABILITY
            ));
        }

        return limitRecommendations(recommendations);
    }

    private void addRecommendation(Map<String, IncomeRecommendation> recommendations,
                                   IncomeRecommendation recommendation) {
        if (recommendation == null || recommendation.getTitle() == null || recommendation.getTitle().isBlank()) {
            return;
        }

        recommendations.putIfAbsent(recommendation.getTitle(), recommendation);
    }

    private List<IncomeRecommendation> limitRecommendations(Map<String, IncomeRecommendation> recommendations) {
        List<IncomeRecommendation> limitedRecommendations = new ArrayList<>();
        for (IncomeRecommendation recommendation : recommendations.values()) {
            if (limitedRecommendations.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
            limitedRecommendations.add(recommendation);
        }
        return limitedRecommendations;
    }

    private IncomeRecommendation recommendation(String title, String description, String priority, String type) {
        return new IncomeRecommendation(title, description, priority, type);
    }

    private void addPrimaryCriticalRecommendation(Map<String, IncomeRecommendation> recommendations,
                                                  String primaryIncomeType) {
        if (IncomeTypes.FREELANCE.equals(primaryIncomeType)) {
            addRecommendation(recommendations, recommendation(
                    "Prevoir un buffer financier freelance",
                    "Vos revenus semblent relies a une activite freelance. Garder plusieurs mois de charges en reserve peut lisser les variations d'encaissement.",
                    HIGH,
                    RISK
            ));
        }

        if (IncomeTypes.CASH_DEPOSIT.equals(primaryIncomeType)) {
            addRecommendation(recommendations, recommendation(
                    "Ameliorer la tracabilite des depots",
                    "Une meilleure tracabilite de vos depots d'especes facilite le pilotage de vos revenus et la preparation de vos justificatifs.",
                    HIGH,
                    RISK
            ));
        }
    }

    private void addPrimaryComplementaryRecommendation(Map<String, IncomeRecommendation> recommendations,
                                                       String primaryIncomeType) {
        if (IncomeTypes.FREELANCE.equals(primaryIncomeType)) {
            addRecommendation(recommendations, recommendation(
                    "Suivre vos revenus irreguliers",
                    "Un suivi mensuel de vos missions et encaissements peut vous aider a mieux anticiper vos periodes creuses et vos pics d'activite.",
                    MEDIUM,
                    OPTIMIZATION
            ));
        }

        if (IncomeTypes.CASH_DEPOSIT.equals(primaryIncomeType)) {
            addRecommendation(recommendations, recommendation(
                    "Renforcer la bancarisation de vos revenus",
                    "Canaliser davantage vos revenus vers des flux bancaires identifies peut simplifier vos analyses et vos futurs projets financiers.",
                    MEDIUM,
                    OPTIMIZATION
            ));
        }
    }

    private void addStabilityCriticalRecommendation(Map<String, IncomeRecommendation> recommendations,
                                                    String incomeStability) {
        if ("STABLE".equals(incomeStability)) {
            addRecommendation(recommendations, recommendation(
                    "Renforcer votre epargne automatique",
                    "Vos revenus paraissent stables. Vous pouvez augmenter progressivement votre epargne reguliere pour securiser vos projets futurs.",
                    HIGH,
                    SAVING
            ));
        }

        if ("VARIABLE".equals(incomeStability)) {
            addRecommendation(recommendations, recommendation(
                    "Constituer un fonds de securite",
                    "Vos revenus paraissent variables. Un matelas de securite peut aider a absorber les mois moins favorables.",
                    HIGH,
                    RISK
            ));
        }
    }

    private void addStabilityComplementaryRecommendation(Map<String, IncomeRecommendation> recommendations,
                                                         String incomeStability) {
        if ("STABLE".equals(incomeStability)) {
            addRecommendation(recommendations, recommendation(
                    "Etudier un investissement progressif",
                    "La stabilite de vos revenus permet d'envisager des placements progressifs et adaptes a vos objectifs.",
                    MEDIUM,
                    OPTIMIZATION
            ));
        }

        if ("VARIABLE".equals(incomeStability)) {
            addRecommendation(recommendations, recommendation(
                    "Adopter une gestion prudente des depenses",
                    "Il est preferable d'ajuster vos engagements fixes et votre budget a un niveau prudent tant que vos revenus restent irreguliers.",
                    HIGH,
                    STABILITY
            ));
        }
    }

    private String buildDefaultDescription(IncomeInsightResponse insight) {
        if (insight.getInsightSummary() != null && !insight.getInsightSummary().isBlank()) {
            return insight.getInsightSummary();
        }

        return "Les informations disponibles ne permettent pas encore de recommander une action plus ciblee.";
    }

    private String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}
