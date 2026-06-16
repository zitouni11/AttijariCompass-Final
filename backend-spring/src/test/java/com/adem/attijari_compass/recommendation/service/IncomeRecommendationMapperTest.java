package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.dto.income.IncomeInsightResponse;
import com.adem.attijari_compass.dto.income.IncomeRecommendation;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import com.adem.attijari_compass.recommendation.enums.RecommendationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IncomeRecommendationMapperTest {

    private final IncomeRecommendationMapper mapper = new IncomeRecommendationMapper();

    @Test
    void shouldMapIncomeRecommendationToGlobalRecommendationFormat() {
        IncomeRecommendation incomeRecommendation = new IncomeRecommendation(
                "Prevoir un buffer financier freelance",
                "Vos revenus semblent relies a une activite freelance.",
                "HIGH",
                "RISK"
        );
        IncomeInsightResponse insight = new IncomeInsightResponse();
        insight.setIncomeConfidenceScore(74);
        insight.setInsightSummary("Vos revenus paraissent variables.");
        insight.setPrimaryIncomeType("freelance");
        insight.setIncomeStability("VARIABLE");
        insight.setIncomeRegularity("IRREGULAR");

        RecommendationDto recommendationDto = mapper.toRecommendationDto(incomeRecommendation, insight);

        assertEquals("Prevoir un buffer financier freelance", recommendationDto.getTitle());
        assertEquals("Prevoir un buffer financier freelance", recommendationDto.getCategory());
        assertEquals("Vos revenus semblent relies a une activite freelance.", recommendationDto.getMessage());
        assertEquals("Reduire votre exposition au risque de revenu", recommendationDto.getSuggestedAction());
        assertEquals(RecommendationPriority.HIGH, recommendationDto.getPriority());
        assertEquals(RecommendationType.RISK_PREVENTION, recommendationDto.getType());
        assertEquals(RecommendationSourceType.INCOME.name(), recommendationDto.getSourceType());
        assertEquals(74.0d, recommendationDto.getConfidenceScore());
        assertNull(recommendationDto.getEstimatedMonthlyGain());
    }
}
