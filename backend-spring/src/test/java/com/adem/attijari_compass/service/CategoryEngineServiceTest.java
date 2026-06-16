package com.adem.attijari_compass.service;

import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryEngineServiceTest {

    private final CategoryEngineService categoryEngineService = new CategoryEngineService();

    @Test
    void shouldCategorizeNetflixAsDivertissement() {
        assertCategory("Netflix", "", TransactionCategory.DIVERTISSEMENT);
        assertCategory("", "Abonnement Netflix", TransactionCategory.DIVERTISSEMENT);
    }

    @Test
    void shouldCategorizeSalaryKeywordsAsSalaire() {
        assertCategory("", "Salaire juin", TransactionCategory.SALAIRE);
        assertCategory("", "Salaire mai", TransactionCategory.SALAIRE);
    }

    @Test
    void shouldCategorizeRestaurantKeywordsAsRestaurant() {
        assertCategory("McDo", "", TransactionCategory.RESTAURANT);
        assertCategory("", "Restaurant terrasse", TransactionCategory.RESTAURANT);
    }

    @Test
    void shouldCategorizeAutoInsuranceAsServiceAuto() {
        assertCategory("", "Assurance voiture", TransactionCategory.SERVICE_AUTO);
        assertCategory("", "Assurance auto annuelle", TransactionCategory.SERVICE_AUTO);
    }

    @Test
    void shouldCategorizeGymAsSante() {
        assertCategory("", "Abonnement salle sport", TransactionCategory.SANTE);
        assertCategory("", "Gym membership", TransactionCategory.SANTE);
    }

    @Test
    void shouldCategorizeFuelAsStationServices() {
        assertCategory("", "Essence BP", TransactionCategory.STATION_SERVICES);
    }

    @Test
    void shouldCategorizeSupermarketAliases() {
        assertCategory("Carrefour", "", TransactionCategory.SUPERMARCHE);
        assertCategory("", "Monoprix La Marsa", TransactionCategory.SUPERMARCHE);
    }

    @Test
    void shouldCategorizeBeautyAndHousingKeywords() {
        assertCategory("", "Coif Hamma", TransactionCategory.BEAUTE);
        assertCategory("", "Loyer juin", TransactionCategory.LOGEMENT);
    }

    @Test
    void shouldCategorizeShortDeliveryMerchant() {
        CategorizationResult result = categoryEngineService.categorize("foody", "payment card foody tunis");

        assertEquals(TransactionCategory.LIVRAISON, result.getCategory());
        assertEquals("RULE_ENGINE", result.getSource());
        assertTrue(result.getConfidence() >= 0.90d);
        assertTrue(result.getNormalizedText().contains("foody"));
    }

    @Test
    void shouldCategorizeArabicSalaryText() {
        CategorizationResult result = categoryEngineService.categorize("", "\u0631\u0627\u062A\u0628");

        assertEquals(TransactionCategory.AUTRES, result.getCategory());
        assertTrue(result.getConfidence() >= 0.90d);
    }

    @Test
    void shouldCategorizeMixedLanguageTelecomBill() {
        CategorizationResult result = categoryEngineService.categorize(
                "ooredoo",
                "\u0641\u0627\u062A\u0648\u0631\u0629 \u0627\u0646\u062A\u0631\u0646\u062A"
        );

        assertEquals(TransactionCategory.OPERATEURS_TELEPHONIQUES, result.getCategory());
        assertTrue(result.getConfidence() >= 0.90d);
        assertTrue(result.getNormalizedText().contains("\u0641\u0627\u062A\u0648\u0631\u0629"));
    }

    @Test
    void shouldCategorizeFuzzyMerchantTypo() {
        CategorizationResult result = categoryEngineService.categorize("carrefouure", "achat carte");

        assertEquals(TransactionCategory.SUPERMARCHE, result.getCategory());
        assertTrue(result.getConfidence() >= 0.90d);
    }

    @Test
    void shouldUseContextRuleForAjilFuel() {
        CategorizationResult result = categoryEngineService.categorize("ajil", "paiement essence autoroute");

        assertEquals(TransactionCategory.STATION_SERVICES, result.getCategory());
        assertTrue(result.getConfidence() >= 0.90d);
    }

    @Test
    void shouldUseContextRuleForAjilKiosque() {
        CategorizationResult result = categoryEngineService.categorize("ajil", "achat kiosque eau");

        assertEquals(TransactionCategory.ALIMENTATION, result.getCategory());
        assertTrue(result.getConfidence() >= 0.90d);
    }

    @Test
    void shouldUseContextRuleForBarberKeywords() {
        CategorizationResult result = categoryEngineService.categorize("hjema", "haircut beard");

        assertEquals(TransactionCategory.BEAUTE, result.getCategory());
        assertTrue(result.getConfidence() >= 0.90d);
    }

    private void assertCategory(String merchant, String description, TransactionCategory expectedCategory) {
        CategorizationResult result = categoryEngineService.categorize(merchant, description);

        assertEquals(expectedCategory, result.getCategory());
        assertEquals("RULE_ENGINE", result.getSource());
        assertTrue(result.getConfidence() >= 0.95d);
        assertTrue(result.getReason() != null && !result.getReason().isBlank());
    }
}
