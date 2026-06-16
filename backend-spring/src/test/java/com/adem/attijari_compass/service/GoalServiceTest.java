package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.goal.GoalAiRefreshResponse;
import com.adem.attijari_compass.dto.goal.GoalAnalysisResponse;
import com.adem.attijari_compass.dto.goal.GoalBlockingCategoryResponse;
import com.adem.attijari_compass.dto.goal.GoalPredictionResponse;
import com.adem.attijari_compass.dto.goal.GoalRecommendationResponse;
import com.adem.attijari_compass.dto.goal.GoalRequest;
import com.adem.attijari_compass.dto.goal.GoalScenarioResponse;
import com.adem.attijari_compass.dto.goal.GoalStorytellingResponse;
import com.adem.attijari_compass.entity.FinancialGoal;
import com.adem.attijari_compass.entity.GoalStatus;
import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.repository.FinancialGoalRepository;
import com.adem.attijari_compass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private FinancialGoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoalAnalysisService goalAnalysisService;

    @Mock
    private GoalPredictionService goalPredictionService;

    @InjectMocks
    private GoalService goalService;

    @Test
    void shouldUpdateGoalAndReturnRefreshedAiPayload() {
        User user = User.builder()
                .id(10L)
                .email("goal@test.com")
                .password("secret")
                .role(Role.USER)
                .build();
        FinancialGoal goal = FinancialGoal.builder()
                .id(42L)
                .name("Voiture")
                .description("Ancien objectif")
                .targetAmount(9_000.0)
                .currentAmount(1_500.0)
                .targetDate(LocalDate.now().plusDays(240))
                .status(GoalStatus.EN_COURS)
                .createdAt(LocalDateTime.of(2026, 1, 10, 8, 30))
                .user(user)
                .build();

        GoalRequest request = new GoalRequest();
        request.setName("Voiture familiale");
        request.setDescription("Objectif revise");
        request.setTargetAmount(12_000.0);
        request.setCurrentAmount(3_000.0);
        request.setTargetDate(LocalDate.now().plusDays(180));

        GoalAnalysisResponse analysis = GoalAnalysisResponse.builder()
                .goalId(42L)
                .balancedSavingCapacity(900.0)
                .prudentSavingCapacity(650.0)
                .aggressiveSavingCapacity(1_200.0)
                .build();
        GoalPredictionResponse prediction = GoalPredictionResponse.builder()
                .goalId(42L)
                .remainingAmount(9_000.0)
                .remainingMonths(6)
                .requiredMonthlySaving(1_500.0)
                .feasibilityScore(68.5)
                .successProbability(76.2)
                .riskLevel("MODERE")
                .predictedAchievementDate(LocalDate.now().plusDays(300))
                .predictedAchievementScenario("EQUILIBRE")
                .recommendedScenario("EQUILIBRE")
                .achievableByTargetDate(false)
                .shortfallAtTargetDate(3_600.0)
                .balancedCapacity(900.0)
                .build();
        GoalBlockingCategoryResponse blocker = GoalBlockingCategoryResponse.builder()
                .category(TransactionCategory.CAFES)
                .categoryLabel("Restaurant")
                .severity("HIGH")
                .severityLabel("Priorite elevee")
                .displayLabel("Restaurant - Priorite elevee")
                .estimatedReducibleAmount(220.0)
                .build();
        List<GoalScenarioResponse> simulations = List.of(
                GoalScenarioResponse.builder()
                        .scenarioName("PRUDENT")
                        .scenarioLabel("Prudent")
                        .suggestedMonthlySaving(650.0)
                        .monthsToReachGoal(14)
                        .predictedAchievementDate(LocalDate.now().plusDays(420))
                        .achievableByTargetDate(false)
                        .shortfallAtTargetDate(5_100.0)
                        .scenarioViabilityScore(43.33)
                        .coverageAtTargetDatePercentage(57.5)
                        .completionPercentageAtTargetDate(43.33)
                        .build(),
                GoalScenarioResponse.builder()
                        .scenarioName("EQUILIBRE")
                        .scenarioLabel("Equilibre")
                        .suggestedMonthlySaving(900.0)
                        .monthsToReachGoal(10)
                        .predictedAchievementDate(LocalDate.now().plusDays(300))
                        .achievableByTargetDate(false)
                        .shortfallAtTargetDate(3_600.0)
                        .scenarioViabilityScore(60.0)
                        .coverageAtTargetDatePercentage(70.0)
                        .completionPercentageAtTargetDate(60.0)
                        .build(),
                GoalScenarioResponse.builder()
                        .scenarioName("AGRESSIF")
                        .scenarioLabel("Agressif")
                        .suggestedMonthlySaving(1_200.0)
                        .monthsToReachGoal(8)
                        .predictedAchievementDate(LocalDate.now().plusDays(240))
                        .achievableByTargetDate(false)
                        .shortfallAtTargetDate(1_800.0)
                        .scenarioViabilityScore(80.0)
                        .coverageAtTargetDatePercentage(85.0)
                        .completionPercentageAtTargetDate(80.0)
                        .build()
        );
        List<GoalRecommendationResponse> recommendations = List.of(
                GoalRecommendationResponse.builder()
                        .type("SPENDING_REDUCTION")
                        .typeLabel("Reduction de depenses")
                        .priority("HIGH")
                        .priorityLabel("Priorite haute")
                        .title("Reduire restaurant")
                        .message("Liberer 220 DT")
                        .estimatedMonthlyImpact(220.0)
                        .build()
        );
        GoalStorytellingResponse storytelling = GoalStorytellingResponse.builder()
                .goalId(42L)
                .realistic(false)
                .statusLabel("difficile")
                .summary("Il reste 9000 DT a financer.")
                .assistantPerspective("Le point de friction principal vient de restaurant.")
                .priorityAction("Reduire les depenses de restaurant.")
                .blockingCategories(List.of("Restaurant"))
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(goalRepository.findByIdAndUserId(goal.getId(), user.getId())).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(FinancialGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goalPredictionService.buildGoalAiSnapshot(goal, user.getEmail())).thenReturn(
                new GoalPredictionService.GoalAiSnapshot(
                        analysis,
                        prediction,
                        List.of(blocker),
                        simulations,
                        recommendations,
                        storytelling
                )
        );

        GoalAiRefreshResponse response = goalService.updateGoalAndRefreshAi(goal.getId(), request, user.getEmail());

        assertNotNull(response);
        assertEquals(goal.getId(), response.getId());
        assertEquals("Voiture familiale", response.getName());
        assertEquals("Objectif revise", response.getDescription());
        assertEquals(12_000.0, response.getTargetAmount());
        assertEquals(3_000.0, response.getCurrentAmount());
        assertEquals(request.getTargetDate(), response.getTargetDate());
        assertEquals(25.0, response.getProgressPercentage());
        assertEquals(9_000.0, response.getRemainingAmount());
        assertEquals(1_500.0, response.getMonthlySavingsRequired());
        assertEquals(prediction.getRequiredMonthlySaving(), response.getRequiredMonthlySaving());
        assertEquals(prediction.getFeasibilityScore(), response.getFeasibilityScore());
        assertEquals(prediction.getSuccessProbability(), response.getSuccessProbability());
        assertEquals(prediction.getRiskLevel(), response.getRiskLevel());
        assertEquals(prediction.getPredictedAchievementDate(), response.getPredictedAchievementDate());
        assertEquals(prediction.getPredictedAchievementScenario(), response.getPredictedAchievementScenario());
        assertEquals(prediction.getRecommendedScenario(), response.getRecommendedScenario());
        assertEquals(prediction.getAchievableByTargetDate(), response.getAchievableByTargetDate());
        assertEquals(prediction.getShortfallAtTargetDate(), response.getShortfallAtTargetDate());
        assertEquals(prediction.getBalancedCapacity(), response.getBalancedCapacity());
        assertEquals(analysis, response.getAnalysis());
        assertEquals(prediction, response.getPrediction());
        assertEquals(List.of(blocker), response.getBlockingCategories());
        assertEquals(simulations, response.getSimulations());
        assertEquals(recommendations, response.getRecommendations());
        assertEquals(storytelling, response.getStorytelling());

        verify(goalRepository).save(goal);
        verify(goalPredictionService).buildGoalAiSnapshot(goal, user.getEmail());
    }
}
