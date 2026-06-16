package com.adem.attijari_compass.recommendation.expense;

import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import com.adem.attijari_compass.recommendation.enums.RecommendationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseRecommendationMapperTest {

    private final ExpenseRecommendationMapper mapper = new ExpenseRecommendationMapper();

    @Test
    void shouldMapExpenseInsightToRecommendationDto() {
        ExpenseInsight insight = ExpenseInsight.builder()
                .insightType("CATEGORY_SPIKE")
                .category(TransactionCategory.SHOPPING)
                .profile(ExpenseCategoryProfile.DISCRETIONARY)
                .title("Maitriser les depenses shopping")
                .message("Les depenses de shopping depassent nettement votre rythme recent.")
                .suggestedAction("Identifier les achats reportables et fixer un plafond sur cette categorie.")
                .priority(RecommendationPriority.HIGH)
                .severityScore(87.5d)
                .confidenceScore(82.0d)
                .estimatedMonthlyGain(150.0d)
                .explanation("Le niveau shopping du mois est de 1157.00 contre une moyenne recente de 749.33.")
                .basedOn(List.of("Seuil de declenchement: 861.73", "Transactions observees ce mois: 4"))
                .build();

        RecommendationDto recommendation = mapper.toRecommendation(insight);

        assertNotNull(recommendation);
        assertEquals("Maitriser les depenses shopping", recommendation.getTitle());
        assertEquals("Les depenses de shopping depassent nettement votre rythme recent.", recommendation.getMessage());
        assertEquals("Identifier les achats reportables et fixer un plafond sur cette categorie.", recommendation.getSuggestedAction());
        assertEquals(RecommendationPriority.HIGH, recommendation.getPriority());
        assertEquals(RecommendationType.HABIT_IMPROVEMENT, recommendation.getType());
        assertEquals("SHOPPING", recommendation.getCategory());
        assertEquals(RecommendationSourceType.EXPENSE.name(), recommendation.getSourceType());
        assertEquals(87.5d, recommendation.getSeverityScore());
        assertEquals(82.0d, recommendation.getConfidenceScore());
        assertEquals(150.0d, recommendation.getEstimatedMonthlyGain());
        assertTrue(Boolean.TRUE.equals(recommendation.getActionable()));
        assertEquals(2, recommendation.getBasedOn().size());
    }

    @Test
    void shouldMapGlobalExpenseInsightWithNullCategory() {
        ExpenseInsight insight = ExpenseInsight.builder()
                .insightType("FIXED_CHARGES_PRESSURE")
                .title("Reduire la pression des charges fixes")
                .message("Vos depenses de logement et factures limitent fortement votre marge mensuelle.")
                .suggestedAction("Identifier les charges incompressibles et les postes eventuellement renegociables.")
                .priority(RecommendationPriority.HIGH)
                .severityScore(92.0d)
                .confidenceScore(88.0d)
                .basedOn(null)
                .build();

        RecommendationDto recommendation = mapper.toRecommendation(insight);

        assertEquals(RecommendationType.RISK_PREVENTION, recommendation.getType());
        assertEquals("CHARGES_FIXES", recommendation.getCategory());
        assertEquals(RecommendationSourceType.EXPENSE.name(), recommendation.getSourceType());
        assertTrue(recommendation.getBasedOn().isEmpty());
    }
}
