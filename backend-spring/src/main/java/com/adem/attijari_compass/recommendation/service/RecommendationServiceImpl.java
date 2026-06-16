package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.entity.FinancialGoal;
import com.adem.attijari_compass.entity.GoalStatus;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.recommendation.expense.ExpenseInsightService;
import com.adem.attijari_compass.recommendation.expense.ExpenseMetricsCalculator;
import com.adem.attijari_compass.recommendation.expense.ExpenseMetricsSnapshot;
import com.adem.attijari_compass.recommendation.dto.FinancialScoreBreakdownDto;
import com.adem.attijari_compass.recommendation.expense.ExpenseRecommendationMapper;
import com.adem.attijari_compass.recommendation.dto.FinancialInsightDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationGoalContextDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationResponseDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationSummaryDto;
import com.adem.attijari_compass.recommendation.enums.RecommendationPriority;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import com.adem.attijari_compass.recommendation.storytelling.RecommendationStorytellingDto;
import com.adem.attijari_compass.recommendation.storytelling.RecommendationStorytellingService;
import com.adem.attijari_compass.repository.FinancialGoalRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private static final int MAX_INCOME_RECOMMENDATIONS = 2;
    private static final int MAX_GOAL_RECOMMENDATIONS = 2;

    private final FinancialAnalysisService financialAnalysisService;
    private final RecommendationEngineService recommendationEngineService;
    private final RecommendationNarrativeService recommendationNarrativeService;
    private final IncomeRecommendationSourceService incomeRecommendationSourceService;
    private final ExpenseInsightService expenseInsightService;
    private final ExpenseMetricsCalculator expenseMetricsCalculator;
    private final FinancialScoreService financialScoreService;
    private final ExpenseRecommendationMapper expenseRecommendationMapper;
    private final RecommendationStorytellingService recommendationStorytellingService;
    private final UserRepository userRepository;
    private final FinancialGoalRepository financialGoalRepository;

    @Override
    public RecommendationResponseDto generateRecommendationsForUser(Long userId) {
        FinancialInsightDto insight = financialAnalysisService.analyzeUserFinancials(userId);
        List<RecommendationDto> incomeRecommendations = incomeRecommendationSourceService.generateRecommendationsForUser(userId);
        ExpenseMetricsSnapshot expenseSnapshot = expenseMetricsCalculator.calculate(userId);
        List<RecommendationDto> expenseRecommendations = getExpenseRecommendations(expenseSnapshot);
        return buildResponse(userId, insight, expenseSnapshot, incomeRecommendations, expenseRecommendations);
    }

    @Override
    public RecommendationResponseDto generateRecommendationsForUser(String email) {
        Long userId = resolveUserId(email);
        FinancialInsightDto insight = financialAnalysisService.analyzeUserFinancials(email);
        List<RecommendationDto> incomeRecommendations = incomeRecommendationSourceService.generateRecommendationsForUser(email);
        ExpenseMetricsSnapshot expenseSnapshot = expenseMetricsCalculator.calculate(userId);
        List<RecommendationDto> expenseRecommendations = getExpenseRecommendations(expenseSnapshot);
        return buildResponse(userId, insight, expenseSnapshot, incomeRecommendations, expenseRecommendations);
    }

    private RecommendationResponseDto buildResponse(Long userId,
                                                    FinancialInsightDto insight,
                                                    ExpenseMetricsSnapshot expenseSnapshot,
                                                    List<RecommendationDto> incomeRecommendations,
                                                    List<RecommendationDto> expenseRecommendations) {
        List<RecommendationDto> recommendations = new ArrayList<>(safeRecommendations(
                recommendationEngineService.generateRecommendations(insight)
        ));
        if (expenseRecommendations != null && !expenseRecommendations.isEmpty()) {
            recommendations.addAll(expenseRecommendations);
        }
        if (incomeRecommendations != null && !incomeRecommendations.isEmpty()) {
            recommendations.addAll(incomeRecommendations);
        }

        recommendations = rebalanceRecommendations(recommendations);
        sortRecommendations(recommendations);
        FinancialScoreBreakdownDto scoreBreakdown = financialScoreService.calculate(userId, insight, expenseSnapshot);
        RecommendationSummaryDto summary = recommendationNarrativeService.buildSummary(
                insight,
                scoreBreakdown,
                recommendations
        );
        if (summary != null && scoreBreakdown != null) {
            summary.setFinancialScore(scoreBreakdown.getFinalScore());
            summary.setFinancialScoreLabel(scoreBreakdown.getLabel());
            summary.setFinancialScoreBreakdown(scoreBreakdown);
        }
        GoalContext goalContext = buildGoalContext(userId, recommendations);
        RecommendationResponseDto response = RecommendationResponseDto.builder()
                .summary(summary)
                .recommendations(recommendations)
                .hasActiveGoal(goalContext.hasActiveGoal())
                .priorityGoal(goalContext.priorityGoal())
                .objectiveImpactMonths(goalContext.objectiveImpactMonths())
                .currentGoalDate(goalContext.currentGoalDate())
                .simulatedGoalDate(goalContext.simulatedGoalDate())
                .build();
        log.info("Generating recommendation storytelling: totalRecommendations={}, globalStatus={}",
                recommendations.size(),
                summary != null ? summary.getGlobalStatus() : null);
        RecommendationStorytellingDto storytelling = recommendationStorytellingService.generateStory(response);
        response.setStorytelling(storytelling);
        log.info("Recommendation storytelling attached: present={}", storytelling != null);
        log.debug("Recommendation storytelling content: {}", storytelling);
        return response;
    }

    private List<RecommendationDto> getExpenseRecommendations(ExpenseMetricsSnapshot expenseSnapshot) {
        return expenseRecommendationMapper.toRecommendations(expenseInsightService.generateInsights(expenseSnapshot));
    }

    private GoalContext buildGoalContext(Long userId, List<RecommendationDto> recommendations) {
        LocalDate today = LocalDate.now();
        List<FinancialGoal> activeGoals = financialGoalRepository.findAllByUserId(userId).stream()
                .filter(Objects::nonNull)
                .filter(goal -> goal.getStatus() == GoalStatus.EN_COURS)
                .filter(goal -> goal.getTargetDate() != null && !goal.getTargetDate().isBefore(today))
                .filter(goal -> safe(goal.getTargetAmount()) > safe(goal.getCurrentAmount()))
                .sorted(Comparator
                        .comparing(FinancialGoal::getTargetDate)
                        .thenComparing(FinancialGoal::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        FinancialGoal priorityGoal = activeGoals.isEmpty() ? null : activeGoals.get(0);
        boolean hasActiveGoal = priorityGoal != null;
        RecommendationGoalContextDto priorityGoalDto = hasActiveGoal
                ? RecommendationGoalContextDto.builder()
                .id(priorityGoal.getId())
                .title(priorityGoal.getName())
                .targetAmount(priorityGoal.getTargetAmount())
                .currentAmount(priorityGoal.getCurrentAmount())
                .targetDate(priorityGoal.getTargetDate())
                .build()
                : null;

        int objectiveImpactMonths = hasActiveGoal ? estimateObjectiveImpactMonths(priorityGoal, recommendations) : 0;
        LocalDate currentGoalDate = hasActiveGoal ? priorityGoal.getTargetDate() : null;
        LocalDate simulatedGoalDate = hasActiveGoal && objectiveImpactMonths > 0
                ? priorityGoal.getTargetDate().minusMonths(objectiveImpactMonths)
                : currentGoalDate;

        log.info(
                "Recommendation goal context: userId={}, activeGoalsCount={}, priorityGoalId={}, hasActiveGoal={}, currentGoalDate={}, simulatedGoalDate={}",
                userId,
                activeGoals.size(),
                priorityGoal != null ? priorityGoal.getId() : null,
                hasActiveGoal,
                currentGoalDate,
                simulatedGoalDate
        );

        return new GoalContext(hasActiveGoal, priorityGoalDto, objectiveImpactMonths, currentGoalDate, simulatedGoalDate);
    }

    private int estimateObjectiveImpactMonths(FinancialGoal goal, List<RecommendationDto> recommendations) {
        double totalGain = safeRecommendations(recommendations).stream()
                .map(RecommendationDto::getEstimatedMonthlyGain)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .filter(value -> value > 0.0)
                .sum();
        double remainingAmount = Math.max(0.0, safe(goal.getTargetAmount()) - safe(goal.getCurrentAmount()));
        long monthsUntilTarget = Math.max(
                1L,
                ChronoUnit.MONTHS.between(LocalDate.now().withDayOfMonth(1), goal.getTargetDate().withDayOfMonth(1))
        );

        if (remainingAmount <= 0.0 || totalGain <= 0.0) {
            return 0;
        }

        int estimatedMonths = (int) Math.round((totalGain * monthsUntilTarget) / Math.max(remainingAmount, totalGain));
        return Math.max(0, Math.min(6, estimatedMonths));
    }

    private List<RecommendationDto> rebalanceRecommendations(List<RecommendationDto> recommendations) {
        List<RecommendationDto> sortedRecommendations = new ArrayList<>(safeRecommendations(recommendations));
        sortRecommendations(sortedRecommendations);

        List<RecommendationDto> balancedRecommendations = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        int incomeCount = 0;
        int goalCount = 0;

        for (RecommendationDto recommendation : sortedRecommendations) {
            if (recommendation == null) {
                continue;
            }

            if (!seenKeys.add(buildDeduplicationKey(recommendation))) {
                continue;
            }

            String sourceType = normalizeSourceType(recommendation.getSourceType());
            if (RecommendationSourceType.INCOME.name().equals(sourceType) && incomeCount >= MAX_INCOME_RECOMMENDATIONS) {
                continue;
            }
            if (RecommendationSourceType.GOAL.name().equals(sourceType) && goalCount >= MAX_GOAL_RECOMMENDATIONS) {
                continue;
            }

            balancedRecommendations.add(recommendation);

            if (RecommendationSourceType.INCOME.name().equals(sourceType)) {
                incomeCount++;
            } else if (RecommendationSourceType.GOAL.name().equals(sourceType)) {
                goalCount++;
            }
        }

        return balancedRecommendations;
    }

    private void sortRecommendations(List<RecommendationDto> recommendations) {
        recommendations.sort(
                Comparator.comparing(
                                (RecommendationDto recommendation) -> priorityRank(recommendation.getPriority()),
                                Comparator.reverseOrder()
                        )
                        .thenComparing(RecommendationDto::getSeverityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(
                                recommendation -> sourceRank(recommendation.getSourceType()),
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                recommendation -> normalizeText(recommendation.getTitle()),
                                Comparator.nullsLast(String::compareTo)
                        )
        );
    }

    private Double priorityRank(RecommendationPriority priority) {
        return switch (priority) {
            case CRITICAL -> 4.0d;
            case HIGH -> 3.0d;
            case MEDIUM -> 2.0d;
            case LOW -> 1.0d;
            case null -> 0.0d;
        };
    }

    private Double sourceRank(String sourceType) {
        return switch (normalizeSourceType(sourceType)) {
            case "EXPENSE" -> 4.0d;
            case "GOAL" -> 3.0d;
            case "INCOME" -> 2.0d;
            case "SIMULATION" -> 1.0d;
            default -> 0.0d;
        };
    }

    private Long resolveUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return user.getId();
    }

    private List<RecommendationDto> safeRecommendations(List<RecommendationDto> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return List.of();
        }

        return recommendations.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private String buildDeduplicationKey(RecommendationDto recommendation) {
        return normalizeSourceType(recommendation.getSourceType())
                + "|"
                + normalizeText(recommendation.getCategory())
                + "|"
                + normalizeText(recommendation.getTitle());
    }

    private String normalizeSourceType(String sourceType) {
        return sourceType == null ? "" : sourceType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private double safe(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0d : value;
    }

    private record GoalContext(
            boolean hasActiveGoal,
            RecommendationGoalContextDto priorityGoal,
            int objectiveImpactMonths,
            LocalDate currentGoalDate,
            LocalDate simulatedGoalDate
    ) {
    }
}
