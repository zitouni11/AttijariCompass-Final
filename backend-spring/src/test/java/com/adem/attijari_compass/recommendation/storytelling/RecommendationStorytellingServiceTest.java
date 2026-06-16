package com.adem.attijari_compass.recommendation.storytelling;

import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationResponseDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationSummaryDto;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import com.adem.attijari_compass.service.storytelling.OllamaClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationStorytellingServiceTest {

    @Test
    void shouldGenerateStructuredStoryFromLlmResponse() {
        OllamaClient ollamaClient = mock(OllamaClient.class);
        when(ollamaClient.generateStructuredJson(anyString(), anyString()))
                .thenReturn("""
                        {
                          "summary": "Votre situation est a surveiller.",
                          "mainConcern": "Les charges fixes pesent fortement sur votre budget.",
                          "opportunity": "Plusieurs ajustements peuvent ameliorer votre marge mensuelle.",
                          "action": "Commencez par renegocier les charges les moins flexibles."
                        }
                        """);

        RecommendationStorytellingService service = new RecommendationStorytellingService(ollamaClient, new ObjectMapper());

        RecommendationStorytellingDto storytelling = service.generateStory(sampleResponse());

        assertNotNull(storytelling);
        assertEquals("Votre situation est a surveiller.", storytelling.getSummary());
        assertEquals("Les charges fixes pesent fortement sur votre budget.", storytelling.getMainConcern());
        assertEquals("Plusieurs ajustements peuvent ameliorer votre marge mensuelle.", storytelling.getOpportunity());
        assertEquals("Commencez par renegocier les charges les moins flexibles.", storytelling.getAction());
    }

    @Test
    void shouldParseJsonWrappedInExtraText() {
        OllamaClient ollamaClient = mock(OllamaClient.class);
        when(ollamaClient.generateStructuredJson(anyString(), anyString()))
                .thenReturn("""
                        Voici votre synthese :
                        {
                          "summary": "Votre situation financiere reste stable.",
                          "mainConcern": "Le budget reste sous controle.",
                          "opportunity": "Vous pouvez renforcer votre epargne.",
                          "action": "Automatisez un virement mensuel."
                        }
                        """);

        RecommendationStorytellingService service = new RecommendationStorytellingService(ollamaClient, new ObjectMapper());

        RecommendationStorytellingDto storytelling = service.generateStory(sampleResponse());

        assertEquals("Votre situation financiere reste stable.", storytelling.getSummary());
        assertEquals("Automatisez un virement mensuel.", storytelling.getAction());
    }

    @Test
    void shouldFallbackWhenLlmFails() {
        OllamaClient ollamaClient = mock(OllamaClient.class);
        doThrow(new IllegalStateException("Ollama unavailable"))
                .when(ollamaClient).generateStructuredJson(anyString(), anyString());

        RecommendationStorytellingService service = new RecommendationStorytellingService(ollamaClient, new ObjectMapper());

        RecommendationStorytellingDto storytelling = service.generateStory(sampleResponse());

        assertNotNull(storytelling);
        assertTrue(storytelling.getSummary().contains("surveiller") || storytelling.getSummary().contains("attention"));
        assertEquals("Automatisez votre epargne.", storytelling.getAction());
    }

    @Test
    void shouldKeepParsedFieldsAndCompleteMissingOnesFromFallback() {
        OllamaClient ollamaClient = mock(OllamaClient.class);
        when(ollamaClient.generateStructuredJson(anyString(), anyString()))
                .thenReturn("""
                        {
                          "summary": "Votre situation merite une vigilance ciblee.",
                          "mainConcern": "",
                          "opportunity": "Une meilleure discipline budgetaire peut liberer de la marge."
                        }
                        """);

        RecommendationStorytellingService service = new RecommendationStorytellingService(ollamaClient, new ObjectMapper());

        RecommendationStorytellingDto storytelling = service.generateStory(sampleResponse());

        assertEquals("Votre situation merite une vigilance ciblee.", storytelling.getSummary());
        assertEquals("Les charges fixes restent elevees.", storytelling.getMainConcern());
        assertEquals("Une meilleure discipline budgetaire peut liberer de la marge.", storytelling.getOpportunity());
        assertEquals("Automatisez votre epargne.", storytelling.getAction());
    }

    private RecommendationResponseDto sampleResponse() {
        return RecommendationResponseDto.builder()
                .summary(RecommendationSummaryDto.builder()
                        .globalStatus("A SURVEILLER")
                        .totalRecommendations(3)
                        .highCount(1)
                        .totalEstimatedMonthlyGain(240.0d)
                        .build())
                .recommendations(List.of(
                        RecommendationDto.builder()
                                .title("Reduire les charges fixes")
                                .message("Les charges fixes restent elevees.")
                                .suggestedAction("Automatisez votre epargne.")
                                .priority(RecommendationPriority.HIGH)
                                .build(),
                        RecommendationDto.builder()
                                .title("Limiter le shopping")
                                .message("Les depenses shopping depassent votre baseline.")
                                .suggestedAction("Fixez un plafond mensuel.")
                                .priority(RecommendationPriority.MEDIUM)
                                .build()
                ))
                .build();
    }
}
