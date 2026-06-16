package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.chat.config.GroqProperties;
import com.adem.attijari_compass.chat.service.GroqService;
import com.adem.attijari_compass.chat.service.RagService;
import com.adem.attijari_compass.recommendation.dto.RecommendationExplanationRequest;
import com.adem.attijari_compass.recommendation.dto.RecommendationExplanationResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationExplanationServiceTest {

    private final RagService ragService = mock(RagService.class);
    private final GroqService groqService = mock(GroqService.class);
    private final GroqProperties groqProperties = new GroqProperties();
    private final RecommendationExplanationService service = new RecommendationExplanationService(
            ragService,
            groqService,
            groqProperties
    );

    @Test
    void buildsCoffeeFallback() {
        RecommendationExplanationRequest request = request("Maîtriser les dépenses café", "CAFES", "EXPENSE");
        request.setTargetedTransactionsTotal(700d);
        request.setMonthlyImpactEstimated(187d);

        String fallback = service.buildFallback(request);

        assertTrue(fallback.toLowerCase().contains("caf"));
        assertTrue(fallback.contains("700"));
        assertTrue(fallback.contains("187"));
        assertFalse(fallback.contains("186,67"));
        assertFalse(fallback.isBlank());
    }

    @Test
    void buildsShoppingFallback() {
        String fallback = service.buildFallback(request("Réduire les achats shopping", "SHOPPING", "EXPENSE"));

        assertTrue(fallback.toLowerCase().contains("achats"));
        assertFalse(fallback.isBlank());
    }

    @Test
    void buildsSavingFallback() {
        String fallback = service.buildFallback(request("Renforcer votre épargne", "EPARGNE", "SAVING"));

        assertTrue(fallback.toLowerCase().contains("épargne"));
        assertFalse(fallback.toLowerCase().contains("réduction"));
        assertFalse(fallback.toLowerCase().contains("dépense"));
        assertFalse(fallback.isBlank());
    }

    @Test
    void distinguishesTargetedTotalAndEstimatedImpact() {
        RecommendationExplanationRequest request = request("Maîtriser les dépenses café", "CAFES", "EXPENSE");
        request.setTargetedTransactionsTotal(700d);
        request.setMonthlyImpactEstimated(187d);

        String fallback = service.buildFallback(request);

        assertTrue(fallback.contains("700"));
        assertTrue(fallback.contains("187"));
        assertTrue(fallback.toLowerCase().contains("estim"));
    }

    @Test
    void returnsFallbackWhenAiFails() {
        RecommendationExplanationRequest request = request("Maîtriser les dépenses café", "CAFES", "EXPENSE");
        when(ragService.buildAdaptiveContext(eq(7L), anyString()))
                .thenThrow(new RuntimeException("timeout"));

        RecommendationExplanationResponse response = service.generateRecommendationExplanation(7L, request);

        assertTrue(response.isFallbackUsed());
        assertTrue(response.getExplanation().toLowerCase().contains("caf"));
        assertFalse(response.getExplanation().isBlank());
    }

    @Test
    void responseIsNeverEmptyOnGenericFallback() {
        String fallback = service.buildFallback(request("Action utile", "OTHER", "OTHER"));

        assertFalse(fallback.isBlank());
    }

    private RecommendationExplanationRequest request(String title, String category, String type) {
        return RecommendationExplanationRequest.builder()
                .title(title)
                .category(category)
                .type(type)
                .amount(120.5d)
                .period("30d")
                .monthlyImpactEstimated(120.5d)
                .potentialPercent(30)
                .financialScore(80)
                .globalStatus("CRITICAL")
                .build();
    }
}
