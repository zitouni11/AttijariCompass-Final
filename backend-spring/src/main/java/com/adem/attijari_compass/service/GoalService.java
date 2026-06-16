package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.goal.GoalAnalysisResponse;
import com.adem.attijari_compass.dto.goal.GoalAiRefreshResponse;
import com.adem.attijari_compass.dto.goal.GoalPredictionResponse;
import com.adem.attijari_compass.dto.goal.GoalRecommendationResponse;
import com.adem.attijari_compass.dto.goal.GoalRequest;
import com.adem.attijari_compass.dto.goal.GoalResponse;
import com.adem.attijari_compass.dto.goal.GoalScenarioResponse;
import com.adem.attijari_compass.dto.goal.GoalStorytellingResponse;
import com.adem.attijari_compass.entity.FinancialGoal;
import com.adem.attijari_compass.entity.GoalStatus;
import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.FinancialGoalRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GoalService {

    private final FinancialGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final GoalAnalysisService goalAnalysisService;
    private final GoalPredictionService goalPredictionService;

    public GoalResponse createGoal(GoalRequest request, String email) {
        User user = getUserByEmail(email);
        FinancialGoal goal = FinancialGoal.builder()
                .name(request.getName())
                .description(request.getDescription())
                .targetAmount(request.getTargetAmount())
                .currentAmount(request.getCurrentAmount() != null ? request.getCurrentAmount() : 0.0)
                .targetDate(request.getTargetDate())
                .status(resolveStatus(request.getCurrentAmount(), request.getTargetAmount()))
                .user(user)
                .build();
        return mapToResponse(goalRepository.save(goal));
    }

    public List<GoalResponse> getAllGoals(String email) {
        User user = getUserByEmail(email);
        return goalRepository.findAllByUserId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<GoalResponse> getGoalsByUser(Long requestedUserId, String email) {
        User currentUser = getUserByEmail(email);
        if (!currentUser.getId().equals(requestedUserId) && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You cannot access another user's goals");
        }
        return goalRepository.findAllByUserId(requestedUserId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public GoalResponse getGoalById(Long id, String email) {
        return mapToResponse(getGoalEntity(id, email));
    }

    public GoalResponse updateGoal(Long id, GoalRequest request, String email) {
        FinancialGoal goal = getGoalEntity(id, email);
        goal.setName(request.getName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        if (request.getCurrentAmount() != null) {
            goal.setCurrentAmount(request.getCurrentAmount());
        }
        goal.setTargetDate(request.getTargetDate());
        goal.setStatus(resolveStatus(goal.getCurrentAmount(), goal.getTargetAmount()));
        return mapToResponse(goalRepository.save(goal));
    }

    public GoalAiRefreshResponse updateGoalAndRefreshAi(Long id, GoalRequest request, String email) {
        FinancialGoal goal = getGoalEntity(id, email);
        double previousTargetAmount = valueOrZero(goal.getTargetAmount());
        double previousCurrentAmount = valueOrZero(goal.getCurrentAmount());
        LocalDate previousTargetDate = goal.getTargetDate();

        log.info("Goal update requested: goalId={}, userEmail={}, previousTargetAmount={}, previousCurrentAmount={}, previousTargetDate={}, newTargetAmount={}, newCurrentAmount={}, newTargetDate={}",
                id,
                email,
                previousTargetAmount,
                previousCurrentAmount,
                previousTargetDate,
                valueOrZero(request.getTargetAmount()),
                valueOrZero(request.getCurrentAmount()),
                request.getTargetDate());

        goal.setName(request.getName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        if (request.getCurrentAmount() != null) {
            goal.setCurrentAmount(request.getCurrentAmount());
        }
        goal.setTargetDate(request.getTargetDate());
        goal.setStatus(resolveStatus(goal.getCurrentAmount(), goal.getTargetAmount()));

        FinancialGoal savedGoal = goalRepository.save(goal);
        GoalAiRefreshResponse refreshResponse = mapToAiRefreshResponse(savedGoal, email);

        log.info("Goal AI recalculated after update: goalId={}, requiredMonthlySaving={}, feasibilityScore={}, successProbability={}, riskLevel={}, predictedAchievementDate={}",
                savedGoal.getId(),
                refreshResponse.getRequiredMonthlySaving(),
                refreshResponse.getFeasibilityScore(),
                refreshResponse.getSuccessProbability(),
                refreshResponse.getRiskLevel(),
                refreshResponse.getPredictedAchievementDate());

        return refreshResponse;
    }

    public GoalResponse addProgress(Long id, Double amount, String email) {
        if (amount == null || amount <= 0.0) {
            throw new IllegalArgumentException("Progress amount must be positive");
        }

        FinancialGoal goal = getGoalEntity(id, email);
        goal.setCurrentAmount(goal.getCurrentAmount() + amount);
        goal.setStatus(resolveStatus(goal.getCurrentAmount(), goal.getTargetAmount()));
        return mapToResponse(goalRepository.save(goal));
    }

    public void deleteGoal(Long id, String email) {
        goalRepository.delete(getGoalEntity(id, email));
    }

    public GoalAnalysisResponse getGoalAnalysis(Long id, String email) {
        return goalAnalysisService.getGoalAnalysis(getGoalEntity(id, email));
    }

    public GoalPredictionResponse getGoalPrediction(Long id, String email) {
        return goalPredictionService.getGoalPrediction(getGoalEntity(id, email));
    }

    public List<GoalScenarioResponse> getGoalSimulations(Long id, String email) {
        return goalPredictionService.getGoalSimulations(getGoalEntity(id, email));
    }

    public List<GoalRecommendationResponse> getGoalRecommendations(Long id, String email) {
        return goalPredictionService.getGoalRecommendations(getGoalEntity(id, email), email);
    }

    public GoalStorytellingResponse generateGoalStorytelling(Long id, String email) {
        return goalPredictionService.buildStorytelling(getGoalEntity(id, email), email);
    }

    public GoalAiRefreshResponse getGoalAiRefresh(Long id, String email) {
        GoalAiRefreshResponse refreshResponse = mapToAiRefreshResponse(getGoalEntity(id, email), email);
        log.debug("Goal AI payload served: goalId={}, userEmail={}, requiredMonthlySaving={}, feasibilityScore={}, successProbability={}, riskLevel={}, predictedAchievementDate={}",
                id,
                email,
                refreshResponse.getRequiredMonthlySaving(),
                refreshResponse.getFeasibilityScore(),
                refreshResponse.getSuccessProbability(),
                refreshResponse.getRiskLevel(),
                refreshResponse.getPredictedAchievementDate());
        return refreshResponse;
    }

    private GoalResponse mapToResponse(FinancialGoal goal) {
        double progress = goal.getTargetAmount() > 0
                ? (goal.getCurrentAmount() / goal.getTargetAmount()) * 100 : 0;
        double remaining = goal.getTargetAmount() - goal.getCurrentAmount();
        int monthsLeft = calculateRemainingMonths(goal.getTargetDate());
        double monthlySavings = monthsLeft > 0 ? remaining / monthsLeft : remaining;

        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .description(goal.getDescription())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus())
                .createdAt(goal.getCreatedAt())
                .progressPercentage(Math.min(progress, 100.0))
                .remainingAmount(Math.max(remaining, 0.0))
                .monthlySavingsRequired(Math.max(monthlySavings, 0.0))
                .build();
    }

    private GoalAiRefreshResponse mapToAiRefreshResponse(FinancialGoal goal, String email) {
        GoalResponse goalResponse = mapToResponse(goal);
        GoalPredictionService.GoalAiSnapshot aiSnapshot = goalPredictionService.buildGoalAiSnapshot(goal, email);
        GoalAnalysisResponse analysis = aiSnapshot.analysis();
        GoalPredictionResponse prediction = aiSnapshot.prediction();
        List<GoalScenarioResponse> simulations = aiSnapshot.simulations();

        return GoalAiRefreshResponse.builder()
                .id(goalResponse.getId())
                .name(goalResponse.getName())
                .description(goalResponse.getDescription())
                .targetAmount(goalResponse.getTargetAmount())
                .currentAmount(goalResponse.getCurrentAmount())
                .targetDate(goalResponse.getTargetDate())
                .status(goalResponse.getStatus())
                .createdAt(goalResponse.getCreatedAt())
                .progressPercentage(goalResponse.getProgressPercentage())
                .remainingAmount(goalResponse.getRemainingAmount())
                .monthlySavingsRequired(goalResponse.getMonthlySavingsRequired())
                .requiredMonthlySaving(prediction.getRequiredMonthlySaving())
                .feasibilityScore(prediction.getFeasibilityScore())
                .successProbability(prediction.getSuccessProbability())
                .riskLevel(prediction.getRiskLevel())
                .predictedAchievementDate(prediction.getPredictedAchievementDate())
                .predictedAchievementScenario(prediction.getPredictedAchievementScenario())
                .recommendedScenario(prediction.getRecommendedScenario())
                .achievableByTargetDate(prediction.getAchievableByTargetDate())
                .shortfallAtTargetDate(prediction.getShortfallAtTargetDate())
                .balancedCapacity(prediction.getBalancedCapacity())
                .analysis(analysis)
                .prediction(prediction)
                .blockingCategories(aiSnapshot.blockingCategories())
                .simulations(simulations)
                .recommendations(aiSnapshot.recommendations())
                .storytelling(aiSnapshot.storytelling())
                .build();
    }

    private int calculateRemainingMonths(LocalDate targetDate) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
        if (daysLeft <= 0) {
            return 0;
        }
        return (int) Math.max(1L, (long) Math.ceil(daysLeft / 30.0));
    }

    private FinancialGoal getGoalEntity(Long id, String email) {
        User user = getUserByEmail(email);
        return goalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private GoalStatus resolveStatus(Double currentAmount, Double targetAmount) {
        double current = currentAmount != null ? currentAmount : 0.0;
        double target = targetAmount != null ? targetAmount : 0.0;
        return current >= target && target > 0.0 ? GoalStatus.ATTEINT : GoalStatus.EN_COURS;
    }

    private double valueOrZero(Double value) {
        return value != null ? value : 0.0;
    }
}
