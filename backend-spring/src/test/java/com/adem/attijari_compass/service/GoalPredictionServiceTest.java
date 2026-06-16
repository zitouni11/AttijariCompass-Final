package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.goal.GoalAnalysisResponse;
import com.adem.attijari_compass.dto.goal.GoalBlockingCategoryResponse;
import com.adem.attijari_compass.dto.goal.GoalPredictionResponse;
import com.adem.attijari_compass.dto.goal.GoalRecommendationResponse;
import com.adem.attijari_compass.dto.goal.GoalScenarioResponse;
import com.adem.attijari_compass.dto.goal.GoalStorytellingResponse;
import com.adem.attijari_compass.dto.recommendation.RecommendationResponse;
import com.adem.attijari_compass.entity.FinancialGoal;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.repository.FinancialGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalPredictionServiceTest {

    @Mock
    private GoalAnalysisService goalAnalysisService;

    @Mock
    private FinancialGoalRepository financialGoalRepository;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private GoalPredictionService goalPredictionService;

    @Test
    void shouldBuildRealisticPredictionWithCorrectMonthlySavingScoreAndDate() {
        FinancialGoal goal = goal(6_000.0, 0.0, LocalDate.now().plusDays(180));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                4_000.0, 2_800.0, 900.0, 2_200.0, 400.0,
                1_200.0, 1_200.0, 1_500.0,
                800.0, 90.0, 80.0, true
        );

        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(1L);

        GoalPredictionResponse response = goalPredictionService.getGoalPrediction(goal);

        assertEquals(6_000.0, response.getRemainingAmount());
        assertEquals(6, response.getRemainingMonths());
        assertEquals(1_000.0, response.getRequiredMonthlySaving());
        assertEquals(93.4, response.getFeasibilityScore());
        assertEquals(99.0, response.getSuccessProbability());
        assertEquals("FAIBLE", response.getRiskLevel());
        assertEquals("EQUILIBRE", response.getPredictedAchievementScenario());
        assertEquals("PRUDENT", response.getRecommendedScenario());
        assertTrue(response.getAchievableByTargetDate());
        assertEquals(LocalDate.now().plusDays(150), response.getPredictedAchievementDate());
    }

    @Test
    void shouldMarkGoalAsDifficultWhenTargetDateIsTooClose() {
        FinancialGoal goal = goal(5_000.0, 0.0, LocalDate.now().plusDays(30));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                2_500.0, 2_200.0, 1_100.0, 2_100.0, 120.0,
                80.0, 120.0, 180.0,
                50.0, 55.0, 45.0, true
        );

        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(2L);

        GoalPredictionResponse response = goalPredictionService.getGoalPrediction(goal);

        assertEquals(1, response.getRemainingMonths());
        assertEquals(5_000.0, response.getRequiredMonthlySaving());
        assertFalse(response.getAchievableByTargetDate());
        assertTrue(response.getShortfallAtTargetDate() > 4_700.0);
        assertNotNull(response.getPredictedAchievementDate());
        assertTrue(response.getFeasibilityScore() < 35.0);
        assertEquals("CRITIQUE", response.getRiskLevel());
    }

    @Test
    void shouldReturnReachedPredictionWhenCurrentAmountExceedsTarget() {
        FinancialGoal goal = goal(1_000.0, 1_200.0, LocalDate.now().plusDays(90));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                3_000.0, 2_100.0, 900.0, 1_800.0, 100.0,
                200.0, 400.0, 500.0,
                250.0, 80.0, 80.0, true
        );

        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(1L);

        GoalPredictionResponse response = goalPredictionService.getGoalPrediction(goal);

        assertEquals(0.0, response.getRemainingAmount());
        assertEquals(0.0, response.getRequiredMonthlySaving());
        assertEquals(100.0, response.getFeasibilityScore());
        assertEquals(99.0, response.getSuccessProbability());
        assertEquals("ATTEINT", response.getRecommendedScenario());
        assertEquals(LocalDate.now(), response.getPredictedAchievementDate());
    }

    @Test
    void shouldGeneratePrudentBalancedAndAggressiveScenarios() {
        FinancialGoal goal = goal(4_800.0, 0.0, LocalDate.now().plusDays(150));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                4_500.0, 3_000.0, 1_000.0, 2_500.0, 450.0,
                700.0, 1_000.0, 1_300.0,
                650.0, 88.0, 84.0, true
        );

        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(1L);

        List<GoalScenarioResponse> scenarios = goalPredictionService.getGoalSimulations(goal);

        assertEquals(3, scenarios.size());
        assertEquals("PRUDENT", scenarios.get(0).getScenarioName());
        assertEquals("Prudent", scenarios.get(0).getScenarioLabel());
        assertEquals("EQUILIBRE", scenarios.get(1).getScenarioName());
        assertEquals("Equilibre", scenarios.get(1).getScenarioLabel());
        assertEquals("AGRESSIF", scenarios.get(2).getScenarioName());
        assertEquals("Agressif", scenarios.get(2).getScenarioLabel());
        assertTrue(scenarios.get(0).getSuggestedMonthlySaving() < scenarios.get(1).getSuggestedMonthlySaving());
        assertTrue(scenarios.get(1).getSuggestedMonthlySaving() < scenarios.get(2).getSuggestedMonthlySaving());
        assertTrue(scenarios.stream().allMatch(scenario -> scenario.getCoverageAtTargetDatePercentage() != null));
        assertTrue(scenarios.stream().allMatch(scenario -> scenario.getScenarioViabilityScore() != null));
    }

    @Test
    void shouldUseBalancedScenarioDateForPredictedAchievementDate() {
        FinancialGoal goal = goal(6_000.0, 0.0, LocalDate.now().plusDays(360));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                4_200.0, 2_900.0, 1_000.0, 2_400.0, 350.0,
                700.0, 900.0, 1_100.0,
                600.0, 86.0, 81.0, true
        );

        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(1L);

        GoalPredictionResponse response = goalPredictionService.getGoalPrediction(goal);

        assertEquals("PRUDENT", response.getRecommendedScenario());
        assertEquals("EQUILIBRE", response.getPredictedAchievementScenario());
        assertEquals(LocalDate.now().plusDays(200), response.getPredictedAchievementDate());
    }

    @Test
    void shouldFallbackToMinimalCapacitiesWhenAnalysisReturnsZero() {
        FinancialGoal goal = goal(4_000.0, 0.0, LocalDate.now().plusDays(120));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                0.0, 600.0, 0.0, 500.0, 50.0,
                0.0, 0.0, 0.0,
                0.0, 20.0, 30.0, false
        );

        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(1L);

        GoalPredictionResponse response = goalPredictionService.getGoalPrediction(goal);
        List<GoalScenarioResponse> scenarios = goalPredictionService.getGoalSimulations(goal);

        assertEquals(75.0, response.getBalancedCapacity());
        assertNotNull(response.getPredictedAchievementDate());
        assertEquals(3, scenarios.size());
        assertEquals(50.0, scenarios.get(0).getSuggestedMonthlySaving());
        assertEquals(75.0, scenarios.get(1).getSuggestedMonthlySaving());
        assertEquals(100.0, scenarios.get(2).getSuggestedMonthlySaving());
        assertEquals(5.0, scenarios.get(0).getScenarioViabilityScore());
        assertEquals(7.5, scenarios.get(1).getScenarioViabilityScore());
        assertEquals(10.0, scenarios.get(2).getScenarioViabilityScore());
        assertTrue(scenarios.stream().allMatch(scenario -> scenario.getPredictedAchievementDate() != null));
    }

    @Test
    void shouldExposeScenarioViabilitySeparatelyFromCoverageAtTargetDate() {
        FinancialGoal goal = goal(45_000.0, 35_200.0, LocalDate.now().plusDays(450));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                2_917.0, 3_307.0, 1_500.0, 2_900.0, 500.0,
                50.0, 75.0, 100.0,
                0.0, 5.0, 5.0, true
        );

        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(1L);

        GoalPredictionResponse prediction = goalPredictionService.getGoalPrediction(goal);
        List<GoalScenarioResponse> scenarios = goalPredictionService.getGoalSimulations(goal);

        assertTrue(prediction.getFeasibilityScore() < 20.0);
        assertTrue(prediction.getSuccessProbability() < 20.0);
        assertEquals(3, scenarios.size());
        assertEquals(79.89, scenarios.get(0).getCoverageAtTargetDatePercentage());
        assertEquals(7.65, scenarios.get(0).getScenarioViabilityScore());
        assertEquals(80.72, scenarios.get(1).getCoverageAtTargetDatePercentage());
        assertEquals(11.48, scenarios.get(1).getScenarioViabilityScore());
        assertEquals(81.56, scenarios.get(2).getCoverageAtTargetDatePercentage());
        assertEquals(15.31, scenarios.get(2).getScenarioViabilityScore());
        assertEquals(scenarios.get(1).getScenarioViabilityScore(), scenarios.get(1).getCompletionPercentageAtTargetDate());
    }

    @Test
    void shouldGenerateRecommendationsForBlockingCategoriesAndDeadlineAdjustment() {
        FinancialGoal goal = goal(7_000.0, 500.0, LocalDate.now().plusDays(60));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                3_500.0, 3_000.0, 1_500.0, 2_800.0, 600.0,
                200.0, 300.0, 450.0,
                100.0, 70.0, 65.0, true,
                List.of(
                        blocking(TransactionCategory.CAFES, 450.0, 247.5, "HIGH"),
                        blocking(TransactionCategory.SHOPPING, 350.0, 175.0, "MEDIUM")
                )
        );

        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(2L);
        when(recommendationService.getRecommendations("goal@test.com")).thenReturn(List.of(
                RecommendationResponse.builder()
                        .categorie("CAFES")
                        .message("signal")
                        .suggestion("cut")
                        .gainEstimeEnMois(120.0)
                        .priorite("HAUTE")
                        .build()
        ));

        List<GoalRecommendationResponse> recommendations = goalPredictionService.getGoalRecommendations(goal, "goal@test.com");

        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().anyMatch(item -> "SPENDING_REDUCTION".equals(item.getType())));
        assertTrue(recommendations.stream().anyMatch(item -> "DEADLINE".equals(item.getType())));
        assertTrue(recommendations.stream().anyMatch(item -> "PRIORITIZATION".equals(item.getType())));
        assertTrue(recommendations.stream().anyMatch(item -> "GENERAL_SIGNAL".equals(item.getType())));
    }

    @Test
    void shouldBuildFrenchStorytellingFromPredictionAndBlockingCategories() {
        FinancialGoal goal = goal(3_000.0, 500.0, LocalDate.now().plusDays(120));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                3_800.0, 2_700.0, 1_100.0, 2_300.0, 350.0,
                500.0, 700.0, 900.0,
                550.0, 85.0, 82.0, false,
                List.of(blocking(TransactionCategory.CAFES, 300.0, 165.0, "HIGH"))
        );
        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(1L);

        GoalStorytellingResponse response = goalPredictionService.buildStorytelling(goal, "goal@test.com");

        assertNotNull(response);
        assertNotNull(response.getSummary());
        assertTrue(response.getSummary().contains("L'objectif \"Voyage\" est realiste."));
        assertTrue(response.getSummary().contains("Il reste 2500 DT"));
        assertTrue(response.getSummary().contains("mensualite requise de 625 DT"));
        assertTrue(response.getSummary().contains("capacite d'epargne equilibree de 700 DT"));
        assertTrue(response.getSummary().contains("restaurant"));
        assertEquals("realiste", response.getStatusLabel());
        assertNotNull(response.getPriorityAction());
        assertEquals("Programmer un virement automatique de 625 DT par mois jusqu'a la date cible.", response.getPriorityAction());
        assertEquals("La trajectoire actuelle reste coherente si vous maintenez au moins 625 DT d'epargne par mois.",
                response.getAssistantPerspective());
        assertEquals(List.of("Restaurant"), response.getBlockingCategories());
        verifyNoInteractions(recommendationService);
    }

    @Test
    void shouldDescribeCriticalGoalWithConcretePriorityAction() {
        FinancialGoal goal = goal(5_000.0, 0.0, LocalDate.now().plusDays(30));
        GoalAnalysisService.GoalAnalysisSnapshot snapshot = snapshot(
                2_500.0, 2_200.0, 1_100.0, 2_100.0, 120.0,
                80.0, 120.0, 180.0,
                50.0, 55.0, 45.0, true,
                List.of(blocking(TransactionCategory.SHOPPING, 350.0, 175.0, "MEDIUM"))
        );
        when(goalAnalysisService.analyze(goal)).thenReturn(snapshot);
        when(financialGoalRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(2L);

        GoalStorytellingResponse response = goalPredictionService.buildStorytelling(goal, "goal@test.com");

        assertFalse(response.getRealistic());
        assertTrue(response.getSummary().contains("est critique"));
        assertTrue(response.getSummary().contains("Il reste 5000 DT"));
        assertTrue(response.getSummary().contains("mensualite requise de 5000 DT"));
        assertTrue(response.getSummary().contains("capacite d'epargne equilibree de 120 DT"));
        assertTrue(response.getSummary().contains("shopping"));
        assertTrue(response.getSummary().contains("L'ecart a combler est de 4880 DT par mois."));
        assertEquals("critique", response.getStatusLabel());
        assertEquals("Reduire en priorite les depenses de shopping pour liberer environ 175 DT par mois.",
                response.getPriorityAction());
        assertEquals("Le point de friction principal vient de shopping, avec un potentiel de reduction estime a 175 DT par mois.",
                response.getAssistantPerspective());
        verifyNoInteractions(recommendationService);
    }

    private FinancialGoal goal(double targetAmount, double currentAmount, LocalDate targetDate) {
        return FinancialGoal.builder()
                .id(50L)
                .name("Voyage")
                .targetAmount(targetAmount)
                .currentAmount(currentAmount)
                .targetDate(targetDate)
                .user(User.builder().id(1L).email("goal@test.com").build())
                .build();
    }

    private GoalAnalysisService.GoalAnalysisSnapshot snapshot(
            double averageMonthlyIncome,
            double averageMonthlyExpenses,
            double estimatedFixedExpenses,
            double estimatedEssentialExpenses,
            double estimatedCompressibleExpenses,
            double prudentSavingCapacity,
            double balancedSavingCapacity,
            double aggressiveSavingCapacity,
            double averageHistoricalSavings,
            double incomeStabilityScore,
            double expenseStabilityScore,
            boolean enoughData
    ) {
        return snapshot(
                averageMonthlyIncome,
                averageMonthlyExpenses,
                estimatedFixedExpenses,
                estimatedEssentialExpenses,
                estimatedCompressibleExpenses,
                prudentSavingCapacity,
                balancedSavingCapacity,
                aggressiveSavingCapacity,
                averageHistoricalSavings,
                incomeStabilityScore,
                expenseStabilityScore,
                enoughData,
                List.of()
        );
    }

    private GoalAnalysisService.GoalAnalysisSnapshot snapshot(
            double averageMonthlyIncome,
            double averageMonthlyExpenses,
            double estimatedFixedExpenses,
            double estimatedEssentialExpenses,
            double estimatedCompressibleExpenses,
            double prudentSavingCapacity,
            double balancedSavingCapacity,
            double aggressiveSavingCapacity,
            double averageHistoricalSavings,
            double incomeStabilityScore,
            double expenseStabilityScore,
            boolean enoughData,
            List<GoalBlockingCategoryResponse> blockingCategories
    ) {
        return new GoalAnalysisService.GoalAnalysisSnapshot(
                averageMonthlyIncome,
                averageMonthlyExpenses,
                estimatedFixedExpenses,
                estimatedFixedExpenses,
                estimatedEssentialExpenses,
                estimatedCompressibleExpenses,
                estimatedCompressibleExpenses,
                prudentSavingCapacity,
                balancedSavingCapacity,
                aggressiveSavingCapacity,
                averageHistoricalSavings,
                incomeStabilityScore,
                expenseStabilityScore,
                3,
                18,
                enoughData,
                enoughData ? "Analysis built from recent transaction history and categorized spending patterns."
                        : "Analysis is based on limited data. Predictions remain indicative until more transactions are available.",
                blockingCategories
        );
    }

    private GoalBlockingCategoryResponse blocking(
            TransactionCategory category,
            double averageMonthlyAmount,
            double reducibleAmount,
            String severity
    ) {
        return GoalBlockingCategoryResponse.builder()
                .category(category)
                .categoryLabel(categoryLabel(category))
                .averageMonthlyAmount(averageMonthlyAmount)
                .estimatedReducibleAmount(reducibleAmount)
                .reductionRate(50.0)
                .severity(severity)
                .severityLabel(severityLabel(severity))
                .displayLabel(categoryLabel(category) + " - " + severityLabel(severity))
                .reason("blocking")
                .build();
    }

    private String categoryLabel(TransactionCategory category) {
        return switch (category) {
            case ALIMENTATION -> "Alimentation";
            case CAFES -> "Cafes";
            case TRANSPORT -> "Transport";
            case HOTEL -> "Hotel";
            case SANTE -> "Sante";
            case DIVERTISSEMENT -> "Divertissement";
            case SHOPPING -> "Shopping";
            case EDUCATION -> "Education";
            case AUTRES -> "Autres depenses";
            case BANQUE -> "Banque";
            case BEAUTE -> "Beaute";
            case DISTRIBUTION -> "Distribution";
            case IMPORT_EXPORT -> "Import/export";
            case LIVRAISON -> "Livraison";
            case NETTOYAGE -> "Nettoyage";
            case OPERATEURS_TELEPHONIQUES -> "Operateurs telephoniques";
            case SERVICE_AUTO -> "Service auto";
            case STATION_SERVICES -> "Station-services";
            case STEG_SONEDE -> "Steg/Sonede";
            case SUPERMARCHE -> "Supermarche";
            case TECHNOLOGIE -> "Technologie";
            case VOYAGE -> "Voyage";
            case RESTAURANT -> "Restaurant";
            case LOGEMENT -> "Logement";
            case SALAIRE -> "Salaire";
            case EPARGNE -> "Epargne";
            case FACTURES -> "Factures";
        };
    }

    private String severityLabel(String severity) {
        return switch (severity) {
            case "HIGH" -> "Priorite elevee";
            case "MEDIUM" -> "Priorite moderee";
            default -> "Priorite secondaire";
        };
    }
}
