package com.adem.attijari_compass.service.income;

import com.adem.attijari_compass.dto.income.IncomeInsightResponse;
import com.adem.attijari_compass.dto.income.IncomeRecommendation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomeRecommendationServiceImplTest {

    private final IncomeRecommendationServiceImpl service = new IncomeRecommendationServiceImpl();

    @Test
    void shouldGenerateStableIncomeRecommendations() {
        IncomeInsightResponse insight = insight(
                IncomeTypes.SALAIRE,
                "STABLE",
                "MONTHLY",
                95,
                false,
                "Vos revenus semblent principalement stables et domines par un salaire mensuel."
        );

        List<IncomeRecommendation> recommendations = service.generateRecommendations(insight);

        assertEquals(2, recommendations.size());
        assertEquals("Renforcer votre epargne automatique", recommendations.get(0).getTitle());
        assertEquals("HIGH", recommendations.get(0).getPriority());
        assertEquals("SAVING", recommendations.get(0).getType());
        assertTrue(recommendations.stream().anyMatch(recommendation ->
                recommendation.getTitle().contains("investissement")));
    }

    @Test
    void shouldGenerateVariableFreelanceRecommendationsWithSecondaryIncome() {
        IncomeInsightResponse insight = insight(
                IncomeTypes.FREELANCE,
                "VARIABLE",
                "IRREGULAR",
                72,
                true,
                "Vos revenus paraissent variables, avec une forte presence de virements et d'activites freelance."
        );

        List<IncomeRecommendation> recommendations = service.generateRecommendations(insight);

        assertEquals(3, recommendations.size());
        assertTrue(recommendations.stream().anyMatch(recommendation ->
                recommendation.getTitle().contains("fonds de securite")));
        assertTrue(recommendations.stream().anyMatch(recommendation ->
                recommendation.getTitle().contains("buffer financier freelance")));
        assertTrue(recommendations.stream().anyMatch(recommendation ->
                recommendation.getTitle().contains("revenus multiples")));
    }

    @Test
    void shouldGenerateCashDepositRecommendations() {
        IncomeInsightResponse insight = insight(
                IncomeTypes.CASH_DEPOSIT,
                "VARIABLE",
                "UNKNOWN",
                60,
                false,
                "Vos revenus reposent surtout sur des depots d'especes."
        );

        List<IncomeRecommendation> recommendations = service.generateRecommendations(insight);

        assertEquals(3, recommendations.size());
        assertTrue(recommendations.stream().anyMatch(recommendation ->
                recommendation.getTitle().contains("tracabilite")));
        assertTrue(recommendations.stream().anyMatch(recommendation ->
                recommendation.getTitle().contains("bancarisation")));
    }

    private IncomeInsightResponse insight(String primaryIncomeType,
                                          String incomeStability,
                                          String incomeRegularity,
                                          Integer incomeConfidenceScore,
                                          boolean hasSecondaryIncome,
                                          String insightSummary) {
        return new IncomeInsightResponse(
                primaryIncomeType,
                incomeStability,
                incomeRegularity,
                incomeConfidenceScore,
                0,
                0,
                0,
                0,
                0,
                0.0d,
                hasSecondaryIncome,
                insightSummary
        );
    }
}
