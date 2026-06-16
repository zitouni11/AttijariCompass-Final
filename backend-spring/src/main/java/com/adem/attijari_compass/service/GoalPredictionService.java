package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.goal.GoalAnalysisResponse;
import com.adem.attijari_compass.dto.goal.GoalBlockingCategoryResponse;
import com.adem.attijari_compass.dto.goal.GoalPredictionResponse;
import com.adem.attijari_compass.dto.goal.GoalRecommendationResponse;
import com.adem.attijari_compass.dto.goal.GoalScenarioResponse;
import com.adem.attijari_compass.dto.goal.GoalStorytellingResponse;
import com.adem.attijari_compass.dto.recommendation.RecommendationResponse;
import com.adem.attijari_compass.entity.FinancialGoal;
import com.adem.attijari_compass.entity.GoalStatus;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.recommendation.enums.RecommendationSourceType;
import com.adem.attijari_compass.repository.FinancialGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalPredictionService {

    private static final double MIN_FALLBACK_CAPACITY = 50.0;
    private static final double BALANCED_FALLBACK_CAPACITY = 75.0;
    private static final double AGGRESSIVE_FALLBACK_CAPACITY = 100.0;

    private final GoalAnalysisService goalAnalysisService;
    private final FinancialGoalRepository financialGoalRepository;
    private final RecommendationService recommendationService;

    public GoalPredictionResponse getGoalPrediction(FinancialGoal goal) {
        return buildGoalAiCoreComputation(goal).prediction();
    }

    public List<GoalScenarioResponse> getGoalSimulations(FinancialGoal goal) {
        return buildGoalAiCoreComputation(goal).simulations();
    }

    public List<GoalRecommendationResponse> getGoalRecommendations(FinancialGoal goal, String email) {
        GoalAiCoreComputation computation = buildGoalAiCoreComputation(goal);
        return buildRecommendations(goal, email, computation.analysisSnapshot(), computation.context(), computation.blockingCategories());
    }

    public GoalStorytellingResponse buildStorytelling(FinancialGoal goal, String email) {
        GoalAiCoreComputation computation = buildGoalAiCoreComputation(goal);
        return buildStorytellingResponse(goal, computation.prediction(), computation.blockingCategories());
    }

    GoalAiSnapshot buildGoalAiSnapshot(FinancialGoal goal, String email) {
        GoalAiCoreComputation computation = buildGoalAiCoreComputation(goal);
        List<GoalRecommendationResponse> recommendations = buildRecommendations(
                goal,
                email,
                computation.analysisSnapshot(),
                computation.context(),
                computation.blockingCategories()
        );
        GoalStorytellingResponse storytelling = buildStorytellingResponse(
                goal,
                computation.prediction(),
                computation.blockingCategories()
        );

        log.debug("Goal AI snapshot built: goalId={}, simulations={}, recommendations={}, blockingCategories={}, storytellingStatus={}",
                goal.getId(),
                computation.simulations().stream()
                        .map(simulation -> simulation.getScenarioName()
                                + "[score=" + simulation.getScenarioViabilityScore()
                                + ", coverage=" + simulation.getCoverageAtTargetDatePercentage()
                                + ", date=" + simulation.getPredictedAchievementDate() + "]")
                        .toList(),
                recommendations.size(),
                computation.blockingCategories().stream()
                        .map(GoalBlockingCategoryResponse::getDisplayLabel)
                        .toList(),
                storytelling.getStatusLabel());

        return new GoalAiSnapshot(
                computation.analysisResponse(),
                computation.prediction(),
                computation.blockingCategories(),
                computation.simulations(),
                recommendations,
                storytelling
        );
    }

    private List<GoalRecommendationResponse> buildRecommendations(
            FinancialGoal goal,
            String email,
            GoalAnalysisService.GoalAnalysisSnapshot analysis,
            PredictionContext context,
            List<GoalBlockingCategoryResponse> blockingCategories
    ) {
        List<GoalRecommendationResponse> recommendations = new ArrayList<>();

        if (context.remainingAmount() <= 0.0) {
            recommendations.add(GoalRecommendationResponse.builder()
                    .type("MAINTAIN")
                    .typeLabel(typeLabel("MAINTAIN"))
                    .priority("LOW")
                    .priorityLabel(priorityLabel("LOW"))
                    .sourceType(RecommendationSourceType.GOAL.name())
                    .title("Objectif deja atteint")
                    .message("Le montant cible est deja couvert. Conservez un versement regulier pour consolider cette habitude d'epargne.")
                    .estimatedMonthlyImpact(0.0)
                    .build());
            return recommendations;
        }

        double monthlyGap = round(Math.max(0.0, context.requiredMonthlySaving() - context.balancedCapacity()));
        if (monthlyGap > 0.0) {
            blockingCategories.stream()
                    .filter(category -> category.getEstimatedReducibleAmount() > 0.0)
                    .limit(2)
                    .forEach(category -> recommendations.add(GoalRecommendationResponse.builder()
                            .type("SPENDING_REDUCTION")
                            .typeLabel(typeLabel("SPENDING_REDUCTION"))
                            .priority(priorityFromSeverity(category.getSeverity()))
                            .priorityLabel(priorityLabel(priorityFromSeverity(category.getSeverity())))
                            .sourceType(RecommendationSourceType.GOAL.name())
                            .title("Reduire " + category.getCategoryLabel().toLowerCase(Locale.ROOT))
                            .message("Cette categorie peut liberer environ " + round(category.getEstimatedReducibleAmount())
                                    + " DT par mois et reduire l'ecart vers la mensualite cible.")
                            .estimatedMonthlyImpact(category.getEstimatedReducibleAmount())
                            .build()));
        }

        GoalScenarioResponse prudentScenario = findScenario(context.scenarios(), "PRUDENT");
        GoalScenarioResponse balancedScenario = findScenario(context.scenarios(), "EQUILIBRE");
        GoalScenarioResponse aggressiveScenario = findScenario(context.scenarios(), "AGRESSIF");

        if (balancedScenario != null && Boolean.TRUE.equals(balancedScenario.getAchievableByTargetDate())) {
            recommendations.add(GoalRecommendationResponse.builder()
                    .type("SCENARIO")
                    .typeLabel(typeLabel("SCENARIO"))
                    .priority("MEDIUM")
                    .priorityLabel(priorityLabel("MEDIUM"))
                    .sourceType(RecommendationSourceType.GOAL.name())
                    .title("Privilegier le scenario equilibre")
                    .message("Le scenario equilibre permet d'atteindre l'objectif sans mobiliser tout le budget flexible.")
                    .estimatedMonthlyImpact(balancedScenario.getSuggestedMonthlySaving())
                    .build());
        } else if (aggressiveScenario != null && Boolean.TRUE.equals(aggressiveScenario.getAchievableByTargetDate())) {
            recommendations.add(GoalRecommendationResponse.builder()
                    .type("SCENARIO")
                    .typeLabel(typeLabel("SCENARIO"))
                    .priority("HIGH")
                    .priorityLabel(priorityLabel("HIGH"))
                    .sourceType(RecommendationSourceType.GOAL.name())
                    .title("Passer temporairement en mode agressif")
                    .message("Le scenario agressif couvre la mensualite requise, au prix d'un arbitrage plus fort sur les depenses compressibles.")
                    .estimatedMonthlyImpact(aggressiveScenario.getSuggestedMonthlySaving())
                    .build());
        }

        if (aggressiveScenario != null && Boolean.FALSE.equals(aggressiveScenario.getAchievableByTargetDate())) {
            int additionalMonths = Math.max(1,
                    Objects.requireNonNullElse(aggressiveScenario.getMonthsToReachGoal(), context.remainingMonths()) - context.remainingMonths());
            recommendations.add(GoalRecommendationResponse.builder()
                    .type("DEADLINE")
                    .typeLabel(typeLabel("DEADLINE"))
                    .priority("HIGH")
                    .priorityLabel(priorityLabel("HIGH"))
                    .sourceType(RecommendationSourceType.GOAL.name())
                    .title("Revoir la date cible")
                    .message("Avec les capacites actuelles, il faut prolonger l'echeance d'environ "
                            + additionalMonths + " mois pour rendre l'objectif plus realiste.")
                    .estimatedMonthlyImpact(0.0)
                    .build());
        }

        long activeGoals = financialGoalRepository.countByUserIdAndStatus(goal.getUser().getId(), GoalStatus.EN_COURS);
        if (activeGoals > 1) {
            recommendations.add(GoalRecommendationResponse.builder()
                    .type("PRIORITIZATION")
                    .typeLabel(typeLabel("PRIORITIZATION"))
                    .priority("MEDIUM")
                    .priorityLabel(priorityLabel("MEDIUM"))
                    .sourceType(RecommendationSourceType.GOAL.name())
                    .title("Prioriser vos objectifs actifs")
                    .message("Vous avez " + activeGoals + " objectifs actifs. Prioriser cet objectif peut eviter de diluer votre capacite d'epargne.")
                    .estimatedMonthlyImpact(0.0)
                    .build());
        }

        if (email != null && !email.isBlank()) {
            List<RecommendationResponse> generalRecommendations = recommendationService.getRecommendations(email);
            generalRecommendations.stream()
                    .filter(recommendation -> !"GENERAL".equalsIgnoreCase(recommendation.getCategorie()))
                    .limit(2)
                    .forEach(recommendation -> {
                        String mappedPriority = mapPriority(recommendation.getPriorite());
                        recommendations.add(GoalRecommendationResponse.builder()
                                .type("GENERAL_SIGNAL")
                                .typeLabel(typeLabel("GENERAL_SIGNAL"))
                                .priority(mappedPriority)
                                .priorityLabel(priorityLabel(mappedPriority))
                                .sourceType(RecommendationSourceType.GOAL.name())
                                .title("Alerte budget " + recommendation.getCategorie())
                                .message("Le moteur de recommandations existant signale aussi cette categorie comme levier pour renforcer l'objectif.")
                                .estimatedMonthlyImpact(round(recommendation.getGainEstimeEnMois()))
                                .build());
                    });
        }

        if (recommendations.isEmpty()) {
            recommendations.add(GoalRecommendationResponse.builder()
                    .type("MAINTAIN")
                    .typeLabel(typeLabel("MAINTAIN"))
                    .priority("LOW")
                    .priorityLabel(priorityLabel("LOW"))
                    .sourceType(RecommendationSourceType.GOAL.name())
                    .title("Cap maintenu")
                    .message("Le rythme actuel est coherent avec l'objectif. Continuez avec un versement mensuel stable.")
                    .estimatedMonthlyImpact(round(context.balancedCapacity()))
                    .build());
        }

        return recommendations;
    }

    private GoalStorytellingResponse buildStorytellingResponse(
            FinancialGoal goal,
            GoalPredictionResponse prediction,
            List<GoalBlockingCategoryResponse> blockingCategories
    ) {
        String statusLabel = storytellingStatus(prediction);

        return GoalStorytellingResponse.builder()
                .goalId(goal.getId())
                .realistic(Boolean.TRUE.equals(prediction.getAchievableByTargetDate()))
                .statusLabel(statusLabel)
                .summary(buildSummary(goal, prediction, blockingCategories))
                .assistantPerspective(buildAssistantPerspective(prediction, blockingCategories))
                .priorityAction(buildPriorityAction(prediction, blockingCategories))
                .blockingCategories(blockingCategories.stream()
                        .map(GoalBlockingCategoryResponse::getCategoryLabel)
                        .toList())
                .build();
    }

    private String buildSummary(
            FinancialGoal goal,
            GoalPredictionResponse prediction,
            List<GoalBlockingCategoryResponse> blockingCategories
    ) {
        String status = storytellingStatus(prediction);
        StringBuilder summary = new StringBuilder()
                .append("L'objectif \"")
                .append(goal.getName())
                .append("\" est ")
                .append(status)
                .append(". Il reste ")
                .append(formatAmount(prediction.getRemainingAmount()))
                .append(" DT a financer");

        if (prediction.getRemainingMonths() != null && prediction.getRemainingMonths() > 0) {
            summary.append(" sur ")
                    .append(prediction.getRemainingMonths())
                    .append(" mois");
        }

        summary.append(", avec une mensualite requise de ")
                .append(formatAmount(prediction.getRequiredMonthlySaving()))
                .append(" DT et une capacite d'epargne equilibree de ")
                .append(formatAmount(prediction.getBalancedCapacity()))
                .append(" DT par mois.");

        if (!blockingCategories.isEmpty()) {
            summary.append(" Les categories bloquantes principales sont ")
                    .append(formatBlockingCategories(blockingCategories, 2))
                    .append(".");
        }

        if (!Boolean.TRUE.equals(prediction.getAchievableByTargetDate()) && prediction.getRemainingAmount() > 0.0) {
            summary.append(" L'ecart a combler est de ")
                    .append(formatAmount(monthlyGap(prediction)))
                    .append(" DT par mois.");
        }

        return summary.toString();
    }

    private String buildAssistantPerspective(
            GoalPredictionResponse prediction,
            List<GoalBlockingCategoryResponse> blockingCategories
    ) {
        if (prediction.getRemainingAmount() <= 0.0) {
            return "Le cap est atteint. La priorite est maintenant de conserver une habitude d'epargne reguliere.";
        }

        if (Boolean.TRUE.equals(prediction.getAchievableByTargetDate())) {
            return "La trajectoire actuelle reste coherente si vous maintenez au moins "
                    + formatAmount(prediction.getRequiredMonthlySaving())
                    + " DT d'epargne par mois.";
        }

        if (!blockingCategories.isEmpty()) {
            GoalBlockingCategoryResponse mainBlocker = blockingCategories.get(0);
            return "Le point de friction principal vient de "
                    + mainBlocker.getCategoryLabel().toLowerCase(Locale.ROOT)
                    + ", avec un potentiel de reduction estime a "
                    + formatAmount(mainBlocker.getEstimatedReducibleAmount())
                    + " DT par mois.";
        }

        return "L'effort demande depasse la capacite d'epargne actuelle de "
                + formatAmount(monthlyGap(prediction))
                + " DT par mois.";
    }

    private String buildPriorityAction(
            GoalPredictionResponse prediction,
            List<GoalBlockingCategoryResponse> blockingCategories
    ) {
        if (prediction.getRemainingAmount() <= 0.0) {
            return "Maintenir un virement automatique symbolique pour consolider l'habitude d'epargne.";
        }

        if (Boolean.TRUE.equals(prediction.getAchievableByTargetDate())) {
            return "Programmer un virement automatique de "
                    + formatAmount(prediction.getRequiredMonthlySaving())
                    + " DT par mois jusqu'a la date cible.";
        }

        if (!blockingCategories.isEmpty()) {
            GoalBlockingCategoryResponse mainBlocker = blockingCategories.get(0);
            return "Reduire en priorite les depenses de "
                    + mainBlocker.getCategoryLabel().toLowerCase(Locale.ROOT)
                    + " pour liberer environ "
                    + formatAmount(mainBlocker.getEstimatedReducibleAmount())
                    + " DT par mois.";
        }

        return "Revoir la date cible, car il manque encore "
                + formatAmount(monthlyGap(prediction))
                + " DT par mois par rapport a la capacite d'epargne actuelle.";
    }

    private List<GoalBlockingCategoryResponse> safeBlockingCategories(GoalAnalysisResponse analysis) {
        if (analysis == null || analysis.getBlockingCategories() == null) {
            return List.of();
        }
        return analysis.getBlockingCategories();
    }

    private String storytellingStatus(GoalPredictionResponse prediction) {
        if (prediction.getRemainingAmount() <= 0.0) {
            return "deja atteint";
        }
        if (Boolean.TRUE.equals(prediction.getAchievableByTargetDate())) {
            return "realiste";
        }
        if ("CRITIQUE".equalsIgnoreCase(prediction.getRiskLevel())) {
            return "critique";
        }
        return "difficile";
    }

    private double monthlyGap(GoalPredictionResponse prediction) {
        return round(Math.max(0.0, safeValue(prediction.getRequiredMonthlySaving()) - safeValue(prediction.getBalancedCapacity())));
    }

    private double safeValue(Double value) {
        return value != null ? value : 0.0;
    }

    private String formatAmount(Double amount) {
        return formatNumber(safeValue(amount));
    }

    private String formatNumber(double value) {
        double roundedValue = round(value);
        if (Math.abs(roundedValue - Math.rint(roundedValue)) < 0.000001) {
            return String.valueOf((long) Math.rint(roundedValue));
        }
        return String.valueOf(roundedValue);
    }

    private String formatBlockingCategories(List<GoalBlockingCategoryResponse> blockingCategories, int limit) {
        List<String> categories = blockingCategories.stream()
                .limit(limit)
                .map(GoalBlockingCategoryResponse::getCategoryLabel)
                .map(label -> label.toLowerCase(Locale.ROOT))
                .toList();

        if (categories.isEmpty()) {
            return "";
        }
        if (categories.size() == 1) {
            return categories.get(0);
        }
        return String.join(", ", categories.subList(0, categories.size() - 1))
                + " et "
                + categories.get(categories.size() - 1);
    }

    private String formatCategory(TransactionCategory category) {
        if (category == null) {
            return "depenses variables";
        }
        return category == TransactionCategory.AUTRES ? "autres depenses" : category.lowerLabel();
    }

    private GoalAiCoreComputation buildGoalAiCoreComputation(FinancialGoal goal) {
        GoalAnalysisService.GoalAnalysisSnapshot analysisSnapshot = goalAnalysisService.analyze(goal);
        GoalAnalysisResponse analysisResponse = goalAnalysisService.mapToResponse(goal.getId(), analysisSnapshot);
        PredictionContext context = buildPredictionContext(goal, analysisSnapshot);
        GoalPredictionResponse prediction = toPredictionResponse(goal, context);
        List<GoalBlockingCategoryResponse> blockingCategories = analysisSnapshot.blockingCategories() != null
                ? analysisSnapshot.blockingCategories()
                : safeBlockingCategories(analysisResponse);

        log.debug("Goal AI core computed: goalId={}, remainingAmount={}, requiredMonthlySaving={}, balancedCapacity={}, recommendedScenario={}, predictedAchievementScenario={}, blockingCategories={}",
                goal.getId(),
                prediction.getRemainingAmount(),
                prediction.getRequiredMonthlySaving(),
                prediction.getBalancedCapacity(),
                prediction.getRecommendedScenario(),
                prediction.getPredictedAchievementScenario(),
                blockingCategories.stream()
                        .map(GoalBlockingCategoryResponse::getDisplayLabel)
                        .toList());

        return new GoalAiCoreComputation(
                analysisSnapshot,
                analysisResponse,
                context,
                prediction,
                blockingCategories
        );
    }

    private GoalPredictionResponse toPredictionResponse(FinancialGoal goal, PredictionContext context) {
        LocalDate predictedAchievementDate = resolvePredictedAchievementDate(context.scenarios(), context.recommendedScenario());
        String predictedAchievementScenario = resolvePredictedAchievementScenario(context.scenarios(), context.recommendedScenario());

        log.debug("Goal prediction computed: goalId={}, requiredMonthlySaving={}, feasibilityScore={}, successProbability={}, riskLevel={}, recommendedScenario={}, uiPredictedAchievementDate={}, predictedAchievementScenario={}",
                goal.getId(),
                context.requiredMonthlySaving(),
                context.feasibilityScore(),
                context.successProbability(),
                context.riskLevel(),
                context.recommendedScenario().getScenarioName(),
                predictedAchievementDate,
                predictedAchievementScenario);

        return GoalPredictionResponse.builder()
                .goalId(goal.getId())
                .remainingAmount(context.remainingAmount())
                .remainingMonths(context.remainingMonths())
                .requiredMonthlySaving(context.requiredMonthlySaving())
                .feasibilityScore(context.feasibilityScore())
                .successProbability(context.successProbability())
                .riskLevel(context.riskLevel())
                .predictedAchievementDate(predictedAchievementDate)
                .predictedAchievementScenario(predictedAchievementScenario)
                .recommendedScenario(context.recommendedScenario().getScenarioName())
                .achievableByTargetDate(context.recommendedScenario().getAchievableByTargetDate())
                .shortfallAtTargetDate(context.recommendedScenario().getShortfallAtTargetDate())
                .balancedCapacity(context.balancedCapacity())
                .build();
    }

    private PredictionContext buildPredictionContext(FinancialGoal goal, GoalAnalysisService.GoalAnalysisSnapshot analysis) {
        double remainingAmount = round(Math.max(0.0, goal.getTargetAmount() - goal.getCurrentAmount()));
        int remainingMonths = calculateRemainingMonths(goal.getTargetDate());
        double requiredMonthlySaving = remainingMonths > 0 ? round(remainingAmount / remainingMonths) : round(remainingAmount);
        double prudentCapacity = normalizeCapacity(analysis.prudentSavingCapacity(), remainingAmount, MIN_FALLBACK_CAPACITY);
        double balancedCapacity = Math.max(prudentCapacity,
                normalizeCapacity(analysis.balancedSavingCapacity(), remainingAmount, BALANCED_FALLBACK_CAPACITY));
        double aggressiveCapacity = Math.max(balancedCapacity,
                normalizeCapacity(analysis.aggressiveSavingCapacity(), remainingAmount, AGGRESSIVE_FALLBACK_CAPACITY));

        List<GoalScenarioResponse> scenarios = List.of(
                buildScenario("PRUDENT", prudentCapacity, goal, remainingAmount, remainingMonths, requiredMonthlySaving),
                buildScenario("EQUILIBRE", balancedCapacity, goal, remainingAmount, remainingMonths, requiredMonthlySaving),
                buildScenario("AGRESSIF", aggressiveCapacity, goal, remainingAmount, remainingMonths, requiredMonthlySaving)
        );

        GoalScenarioResponse recommendedScenario = chooseRecommendedScenario(remainingAmount, scenarios);
        long activeGoals = financialGoalRepository.countByUserIdAndStatus(goal.getUser().getId(), GoalStatus.EN_COURS);
        double feasibilityScore = feasibilityScore(requiredMonthlySaving, analysis, activeGoals, balancedCapacity);
        double successProbability = successProbability(feasibilityScore, requiredMonthlySaving, balancedCapacity);
        String riskLevel = riskLevel(feasibilityScore, recommendedScenario);

        return new PredictionContext(
                remainingAmount,
                remainingMonths,
                requiredMonthlySaving,
                round(feasibilityScore),
                round(successProbability),
                riskLevel,
                balancedCapacity,
                recommendedScenario,
                scenarios
        );
    }

    private double normalizeCapacity(double capacity, double remainingAmount, double fallbackCapacity) {
        if (remainingAmount <= 0.0) {
            return round(Math.max(0.0, capacity));
        }
        return round(Math.max(Math.max(0.0, capacity), fallbackCapacity));
    }

    private LocalDate resolvePredictedAchievementDate(
            List<GoalScenarioResponse> scenarios,
            GoalScenarioResponse recommendedScenario
    ) {
        GoalScenarioResponse balancedScenario = findScenario(scenarios, "EQUILIBRE");
        if (balancedScenario != null && balancedScenario.getPredictedAchievementDate() != null) {
            return balancedScenario.getPredictedAchievementDate();
        }
        return recommendedScenario != null ? recommendedScenario.getPredictedAchievementDate() : null;
    }

    private String resolvePredictedAchievementScenario(
            List<GoalScenarioResponse> scenarios,
            GoalScenarioResponse recommendedScenario
    ) {
        GoalScenarioResponse balancedScenario = findScenario(scenarios, "EQUILIBRE");
        if (balancedScenario != null && balancedScenario.getPredictedAchievementDate() != null) {
            return balancedScenario.getScenarioName();
        }
        return recommendedScenario != null ? recommendedScenario.getScenarioName() : null;
    }

    private GoalScenarioResponse chooseRecommendedScenario(double remainingAmount, List<GoalScenarioResponse> scenarios) {
        if (remainingAmount <= 0.0) {
            return GoalScenarioResponse.builder()
                    .scenarioName("ATTEINT")
                    .sourceType(RecommendationSourceType.SIMULATION.name())
                    .suggestedMonthlySaving(0.0)
                    .monthsToReachGoal(0)
                    .predictedAchievementDate(LocalDate.now())
                    .achievableByTargetDate(true)
                    .shortfallAtTargetDate(0.0)
                    .completionPercentageAtTargetDate(100.0)
                    .build();
        }

        GoalScenarioResponse prudentScenario = findScenario(scenarios, "PRUDENT");
        if (prudentScenario != null && Boolean.TRUE.equals(prudentScenario.getAchievableByTargetDate())) {
            return prudentScenario;
        }

        GoalScenarioResponse balancedScenario = findScenario(scenarios, "EQUILIBRE");
        if (balancedScenario != null && Boolean.TRUE.equals(balancedScenario.getAchievableByTargetDate())) {
            return balancedScenario;
        }

        GoalScenarioResponse aggressiveScenario = findScenario(scenarios, "AGRESSIF");
        if (aggressiveScenario != null && Boolean.TRUE.equals(aggressiveScenario.getAchievableByTargetDate())) {
            return aggressiveScenario;
        }

        return scenarios.stream()
                .filter(scenario -> scenario.getSuggestedMonthlySaving() > 0.0)
                .min(Comparator.comparing(GoalScenarioResponse::getMonthsToReachGoal, Comparator.nullsLast(Integer::compareTo)))
                .orElse(scenarios.get(1));
    }

    private GoalScenarioResponse buildScenario(
            String scenarioName,
            double monthlySaving,
            FinancialGoal goal,
            double remainingAmount,
            int remainingMonths,
            double requiredMonthlySaving
    ) {
        if (remainingAmount <= 0.0) {
            return GoalScenarioResponse.builder()
                    .scenarioName(scenarioName)
                    .scenarioLabel(scenarioLabel(scenarioName))
                    .sourceType(RecommendationSourceType.SIMULATION.name())
                    .suggestedMonthlySaving(round(monthlySaving))
                    .monthsToReachGoal(0)
                    .predictedAchievementDate(LocalDate.now())
                    .achievableByTargetDate(true)
                    .shortfallAtTargetDate(0.0)
                    .scenarioViabilityScore(100.0)
                    .coverageAtTargetDatePercentage(100.0)
                    .completionPercentageAtTargetDate(100.0)
                    .build();
        }

        Integer monthsToReachGoal = monthlySaving > 0.0 ? (int) Math.ceil(remainingAmount / monthlySaving) : null;
        LocalDate predictedAchievementDate = monthlySaving > 0.0
                ? LocalDate.now().plusDays(estimateDaysToGoal(remainingAmount, monthlySaving))
                : null;
        double savedByTarget = goal.getCurrentAmount() + (monthlySaving * remainingMonths);
        double shortfallAtTargetDate = round(Math.max(0.0, goal.getTargetAmount() - savedByTarget));
        double coverageAtTargetDatePercentage = goal.getTargetAmount() > 0.0
                ? round(Math.min(100.0, (savedByTarget / goal.getTargetAmount()) * 100.0))
                : 0.0;
        double scenarioViabilityScore = scenarioViabilityScore(monthlySaving, requiredMonthlySaving);

        return GoalScenarioResponse.builder()
                .scenarioName(scenarioName)
                .scenarioLabel(scenarioLabel(scenarioName))
                .sourceType(RecommendationSourceType.SIMULATION.name())
                .suggestedMonthlySaving(round(monthlySaving))
                .monthsToReachGoal(monthsToReachGoal)
                .predictedAchievementDate(predictedAchievementDate)
                .achievableByTargetDate(monthsToReachGoal != null && monthsToReachGoal <= remainingMonths)
                .shortfallAtTargetDate(shortfallAtTargetDate)
                .scenarioViabilityScore(scenarioViabilityScore)
                .coverageAtTargetDatePercentage(coverageAtTargetDatePercentage)
                // Legacy UI clients still bind this field. Keep it aligned with the scenario score.
                .completionPercentageAtTargetDate(scenarioViabilityScore)
                .build();
    }

    private double feasibilityScore(
            double requiredMonthlySaving,
            GoalAnalysisService.GoalAnalysisSnapshot analysis,
            long activeGoals,
            double balancedCapacity
    ) {
        if (requiredMonthlySaving <= 0.0) {
            return 100.0;
        }

        double capacityCoverage = balancedCapacity / requiredMonthlySaving;
        double capacityScore = Math.min(40.0, Math.max(0.0, capacityCoverage) / 1.25 * 40.0);
        double incomeStabilityScore = analysis.incomeStabilityScore() * 0.20;
        double expenseStabilityScore = analysis.expenseStabilityScore() * 0.15;
        double savingsRate = analysis.averageMonthlyIncome() > 0.0
                ? analysis.averageHistoricalSavings() / analysis.averageMonthlyIncome()
                : 0.0;
        double historyScore = Math.min(15.0, Math.max(0.0, savingsRate) * 75.0);
        double goalLoadScore = Math.max(0.0, 10.0 - Math.max(0L, activeGoals - 1L) * 2.5);

        double score = capacityScore + incomeStabilityScore + expenseStabilityScore + historyScore + goalLoadScore;
        if (!analysis.enoughData()) {
            score = Math.min(score, 65.0);
        }
        return Math.max(0.0, Math.min(100.0, score));
    }

    private double successProbability(double feasibilityScore, double requiredMonthlySaving, double balancedCapacity) {
        if (requiredMonthlySaving <= 0.0) {
            return 99.0;
        }
        double coverageBoost = balancedCapacity > 0.0 ? (balancedCapacity / requiredMonthlySaving) * 20.0 : 0.0;
        return Math.max(5.0, Math.min(99.0, (feasibilityScore * 0.90) + coverageBoost));
    }

    private String riskLevel(double feasibilityScore, GoalScenarioResponse recommendedScenario) {
        if (feasibilityScore >= 75.0 && Boolean.TRUE.equals(recommendedScenario.getAchievableByTargetDate())) {
            return "FAIBLE";
        }
        if (feasibilityScore >= 55.0) {
            return "MODERE";
        }
        if (feasibilityScore >= 35.0) {
            return "ELEVE";
        }
        return "CRITIQUE";
    }

    private GoalScenarioResponse findScenario(List<GoalScenarioResponse> scenarios, String scenarioName) {
        return scenarios.stream()
                .filter(scenario -> scenarioName.equalsIgnoreCase(scenario.getScenarioName()))
                .findFirst()
                .orElse(null);
    }

    private int calculateRemainingMonths(LocalDate targetDate) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
        if (daysLeft <= 0) {
            return 0;
        }
        return (int) Math.max(1L, (long) Math.ceil(daysLeft / 30.0));
    }

    private long estimateDaysToGoal(double remainingAmount, double monthlySaving) {
        if (remainingAmount <= 0.0 || monthlySaving <= 0.0) {
            return 0L;
        }

        long estimatedDays = (long) Math.ceil((remainingAmount / monthlySaving) * 30.0);
        return Math.max(1L, estimatedDays);
    }

    private String priorityFromSeverity(String severity) {
        return switch (severity) {
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private String priorityLabel(String priority) {
        return switch (priority) {
            case "HIGH" -> "Priorite haute";
            case "MEDIUM" -> "Priorite moyenne";
            default -> "Priorite basse";
        };
    }

    private String typeLabel(String type) {
        return switch (type) {
            case "SPENDING_REDUCTION" -> "Reduction de depenses";
            case "SCENARIO" -> "Scenario recommande";
            case "DEADLINE" -> "Ajustement d'echeance";
            case "PRIORITIZATION" -> "Priorisation";
            case "GENERAL_SIGNAL" -> "Signal budget";
            default -> "Suivi";
        };
    }

    private String mapPriority(String priority) {
        if (priority == null) {
            return "LOW";
        }
        return switch (priority.toUpperCase(Locale.ROOT)) {
            case "HAUTE" -> "HIGH";
            case "MOYENNE" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String scenarioLabel(String scenarioName) {
        return switch (scenarioName.toUpperCase(Locale.ROOT)) {
            case "PRUDENT" -> "Prudent";
            case "EQUILIBRE" -> "Equilibre";
            case "AGRESSIF" -> "Agressif";
            case "ATTEINT" -> "Atteint";
            default -> scenarioName;
        };
    }

    private double scenarioViabilityScore(double monthlySaving, double requiredMonthlySaving) {
        if (requiredMonthlySaving <= 0.0) {
            return 100.0;
        }
        return round(Math.max(0.0, Math.min(100.0, (monthlySaving / requiredMonthlySaving) * 100.0)));
    }

    private record PredictionContext(
            double remainingAmount,
            int remainingMonths,
            double requiredMonthlySaving,
            double feasibilityScore,
            double successProbability,
            String riskLevel,
            double balancedCapacity,
            GoalScenarioResponse recommendedScenario,
            List<GoalScenarioResponse> scenarios
    ) {
    }

    record GoalAiSnapshot(
            GoalAnalysisResponse analysis,
            GoalPredictionResponse prediction,
            List<GoalBlockingCategoryResponse> blockingCategories,
            List<GoalScenarioResponse> simulations,
            List<GoalRecommendationResponse> recommendations,
            GoalStorytellingResponse storytelling
    ) {
    }

    private record GoalAiCoreComputation(
            GoalAnalysisService.GoalAnalysisSnapshot analysisSnapshot,
            GoalAnalysisResponse analysisResponse,
            PredictionContext context,
            GoalPredictionResponse prediction,
            List<GoalBlockingCategoryResponse> blockingCategories
    ) {
        private List<GoalScenarioResponse> simulations() {
            return context.scenarios();
        }
    }
}
