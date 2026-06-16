package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.FinancialScoreBreakdownDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationSummaryDto;
import com.adem.attijari_compass.recommendation.enums.CurrentMonthSeverity;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecommendationNarrativeServiceImplTest {

    @Test
    void shouldExposeFinancialScoreInSummary() {
        RecommendationNarrativeServiceImpl service = new RecommendationNarrativeServiceImpl();

        RecommendationSummaryDto summary = service.buildSummary(
                FinancialInsightDto.builder().build(),
                FinancialScoreBreakdownDto.builder()
                        .finalScore(58)
                        .baseScore(100)
                        .penaltyPoints(-42)
                        .bonusPoints(0)
                        .label("A consolider")
                        .currentMonthSeverity(CurrentMonthSeverity.NORMAL)
                        .build(),
                List.of(
                        RecommendationDto.builder()
                                .title("Reduire le shopping")
                                .priority(RecommendationPriority.HIGH)
                                .estimatedMonthlyGain(120.0d)
                                .build()
                )
        );

        assertNotNull(summary);
        assertEquals(58, summary.getFinancialScore());
        assertEquals("A consolider", summary.getFinancialScoreLabel());
        assertEquals(CurrentMonthSeverity.NORMAL, summary.getCurrentMonthSeverity());
        assertEquals("A SURVEILLER", summary.getGlobalStatus());
        assertNotNull(summary.getFinancialScoreBreakdown());
        assertEquals(1, summary.getTotalRecommendations());
        assertEquals("Votre score financier est de 58/100 (A consolider). Plusieurs signaux appellent une vigilance renforcee. 1 recommandation(s) ont ete generees, avec 1 priorite(s) haute(s). Les principaux leviers concernent: Reduire le shopping. Potentiel mensuel estime: 120.00.",
                summary.getAiSummary());
    }
}
