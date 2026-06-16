package com.adem.attijari_compass.service;

import com.adem.attijari_compass.config.CategorizationMlProperties;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.model.categorization.CategorizationSources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartCategorizationServiceTest {

    @Mock
    private CategoryEngineService categoryEngineService;

    @Mock
    private MlCategorizationService mlCategorizationService;

    @Mock
    private TransactionCategoryFeedbackService transactionCategoryFeedbackService;

    private SmartCategorizationService smartCategorizationService;

    @BeforeEach
    void setUp() {
        CategorizationMlProperties properties = new CategorizationMlProperties();
        properties.setEnabled(true);
        properties.setThreshold(0.65d);
        properties.setBaseUrl("http://localhost:8001");
        smartCategorizationService = new SmartCategorizationService(
                categoryEngineService,
                mlCategorizationService,
                properties,
                transactionCategoryFeedbackService
        );
    }

    @Test
    void shouldReturnReliableRuleWithoutCallingMl() {
        when(categoryEngineService.categorize("foody", "payment card foody tunis"))
                .thenReturn(CategorizationResult.builder()
                        .category(TransactionCategory.LIVRAISON)
                        .confidence(0.96d)
                        .source(CategorizationSources.RULE_ENGINE)
                        .normalizedText("foody payment card foody tunis")
                        .build());

        CategorizationResult result = smartCategorizationService.categorize("foody", "payment card foody tunis");

        assertEquals(TransactionCategory.LIVRAISON, result.getCategory());
        assertEquals(CategorizationSources.RULE_ENGINE, result.getSource());
        verifyNoInteractions(mlCategorizationService);
    }

    @Test
    void shouldUseMlWhenRuleConfidenceIsWeak() {
        when(categoryEngineService.categorize("unknown", "uber trip"))
                .thenReturn(CategorizationResult.builder()
                        .category(TransactionCategory.TRANSPORT)
                        .confidence(0.82d)
                        .source(CategorizationSources.RULE_ENGINE)
                        .normalizedText("unknown uber trip")
                        .build());
        when(mlCategorizationService.categorize("unknown", "uber trip"))
                .thenReturn(Optional.of(CategorizationResult.builder()
                        .category(TransactionCategory.TRANSPORT)
                        .confidence(0.91d)
                        .source(CategorizationSources.ML_MODEL)
                        .normalizedText("unknown uber trip")
                        .build()));

        CategorizationResult result = smartCategorizationService.categorize("unknown", "uber trip");

        assertEquals(TransactionCategory.TRANSPORT, result.getCategory());
        assertEquals(CategorizationSources.ML_MODEL, result.getSource());
        assertTrue(result.getConfidence() >= 0.65d);
    }

    @Test
    void shouldReturnAutresWhenMlConfidenceIsBelowThreshold() {
        when(categoryEngineService.categorize("merchant", "description"))
                .thenReturn(CategorizationResult.builder()
                        .category(TransactionCategory.AUTRES)
                        .confidence(0.0d)
                        .source(CategorizationSources.RULE_ENGINE)
                        .normalizedText("merchant description")
                        .build());
        when(mlCategorizationService.categorize("merchant", "description"))
                .thenReturn(Optional.of(CategorizationResult.builder()
                        .category(TransactionCategory.SHOPPING)
                        .confidence(0.45d)
                        .source(CategorizationSources.ML_MODEL)
                        .normalizedText("merchant description")
                        .build()));

        CategorizationResult result = smartCategorizationService.categorize("merchant", "description");

        assertEquals(TransactionCategory.AUTRES, result.getCategory());
        assertEquals(CategorizationSources.FALLBACK, result.getSource());
        assertEquals(0.45d, result.getConfidence());
    }
}
