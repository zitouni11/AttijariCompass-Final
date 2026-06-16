package com.adem.attijari_compass.simulations.service;

import com.adem.attijari_compass.simulations.dto.request.SavingsCalculateRequest;
import com.adem.attijari_compass.simulations.dto.request.SavingsCompareRequest;
import com.adem.attijari_compass.simulations.dto.request.SavingsScenarioRequest;
import com.adem.attijari_compass.simulations.dto.response.SavingsCalculateResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsCompareResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsMilestoneResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsProjectionPointResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsScenarioInputResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsScenarioResult;
import com.adem.attijari_compass.simulations.model.ContributionFrequency;
import com.adem.attijari_compass.simulations.util.SimulationMathUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SavingsSimulationService {

    private static final int MAX_PROJECTION_MONTHS = 120;
    private static final List<Integer> MILESTONES = List.of(25, 50, 75, 100);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public SavingsCalculateResponse calculate(SavingsCalculateRequest request) {
        SavingsSnapshot snapshot = calculateSnapshot(
                request.getTargetAmount(),
                request.getInitialAmount(),
                request.getMonthlyContribution(),
                request.getExtraContribution(),
                request.getContributionFrequency(),
                request.getStartDate(),
                request.getTargetDate()
        );

        return SavingsCalculateResponse.builder()
                .estimatedMonths(snapshot.estimatedMonths())
                .estimatedEndDate(snapshot.estimatedEndDate())
                .totalContributed(snapshot.totalContributed())
                .remainingAmount(snapshot.remainingAmount())
                .milestones(snapshot.milestones())
                .projectionPoints(snapshot.projectionPoints())
                .simulationSummary(snapshot.simulationSummary())
                .build();
    }

    public SavingsCompareResponse compare(SavingsCompareRequest request) {
        List<SavingsScenarioResult> scenarios = request.getScenarios().stream()
                .map(this::calculateScenarioResult)
                .toList();

        return SavingsCompareResponse.builder()
                .scenarios(scenarios)
                .build();
    }

    private SavingsScenarioResult calculateScenarioResult(SavingsScenarioRequest request) {
        SavingsSnapshot snapshot = calculateSnapshot(
                request.getTargetAmount(),
                request.getInitialAmount(),
                request.getMonthlyContribution(),
                request.getExtraContribution(),
                request.getContributionFrequency(),
                request.getStartDate(),
                request.getTargetDate()
        );

        return SavingsScenarioResult.builder()
                .scenarioName(request.getScenarioName())
                .inputs(SavingsScenarioInputResponse.builder()
                        .targetAmount(SimulationMathUtils.money(request.getTargetAmount()))
                        .initialAmount(SimulationMathUtils.money(request.getInitialAmount()))
                        .monthlyContribution(SimulationMathUtils.money(request.getMonthlyContribution()))
                        .extraContribution(SimulationMathUtils.money(request.getExtraContribution()))
                        .contributionFrequency(request.getContributionFrequency())
                        .startDate(SimulationMathUtils.defaultStartDate(request.getStartDate()))
                        .targetDate(request.getTargetDate())
                        .build())
                .estimatedMonths(snapshot.estimatedMonths())
                .estimatedEndDate(snapshot.estimatedEndDate())
                .totalContributed(snapshot.totalContributed())
                .remainingAmount(snapshot.remainingAmount())
                .milestones(snapshot.milestones())
                .build();
    }

    private SavingsSnapshot calculateSnapshot(
            BigDecimal targetAmountRaw,
            BigDecimal initialAmountRaw,
            BigDecimal monthlyContributionRaw,
            BigDecimal extraContributionRaw,
            ContributionFrequency contributionFrequency,
            LocalDate startDateRaw,
            LocalDate targetDate) {
        LocalDate startDate = SimulationMathUtils.defaultStartDate(startDateRaw);
        validateDates(startDate, targetDate);

        BigDecimal targetAmount = SimulationMathUtils.money(targetAmountRaw);
        BigDecimal initialAmount = SimulationMathUtils.money(initialAmountRaw);
        BigDecimal extraContribution = SimulationMathUtils.money(extraContributionRaw);
        BigDecimal monthlyContribution = SimulationMathUtils.money(monthlyContributionRaw);
        BigDecimal startingAmount = SimulationMathUtils.money(initialAmount.add(extraContribution));
        BigDecimal remainingAmount = SimulationMathUtils.money(targetAmount.subtract(startingAmount).max(BigDecimal.ZERO));

        int estimatedMonths = estimateMonths(targetAmount, startingAmount, monthlyContribution, contributionFrequency);
        LocalDate estimatedEndDate = startDate.plusMonths(estimatedMonths);
        BigDecimal totalContributed = projectedAmountAtMonth(
                targetAmount,
                startingAmount,
                monthlyContribution,
                contributionFrequency,
                estimatedMonths,
                estimatedMonths
        );

        int targetDateMonths = targetDate == null ? 0 : SimulationMathUtils.monthsBetween(startDate, targetDate);
        int projectionHorizon = Math.max(estimatedMonths, targetDateMonths);

        return new SavingsSnapshot(
                estimatedMonths,
                estimatedEndDate,
                totalContributed,
                remainingAmount,
                buildMilestones(targetAmount, startingAmount, monthlyContribution, contributionFrequency, startDate, estimatedMonths),
                buildProjectionPoints(targetAmount, startingAmount, monthlyContribution, contributionFrequency, startDate, estimatedMonths, projectionHorizon),
                buildSummary(targetAmount, startingAmount, monthlyContribution, contributionFrequency, estimatedMonths, estimatedEndDate, targetDate)
        );
    }

    private void validateDates(LocalDate startDate, LocalDate targetDate) {
        if (targetDate != null && targetDate.isBefore(startDate)) {
            throw new IllegalArgumentException("targetDate must be on or after startDate");
        }
    }

    private int estimateMonths(
            BigDecimal targetAmount,
            BigDecimal startingAmount,
            BigDecimal monthlyContribution,
            ContributionFrequency contributionFrequency) {
        if (startingAmount.compareTo(targetAmount) >= 0) {
            return 0;
        }

        BigDecimal eventContribution = contributionPerEvent(monthlyContribution, contributionFrequency);
        BigDecimal required = targetAmount.subtract(startingAmount);
        BigDecimal events = required.divide(eventContribution, 0, RoundingMode.CEILING);
        return events.intValueExact() * contributionFrequency.getMonthInterval();
    }

    private List<SavingsMilestoneResponse> buildMilestones(
            BigDecimal targetAmount,
            BigDecimal startingAmount,
            BigDecimal monthlyContribution,
            ContributionFrequency contributionFrequency,
            LocalDate startDate,
            int estimatedMonths) {
        List<SavingsMilestoneResponse> milestones = new ArrayList<>();
        for (Integer percentage : MILESTONES) {
            BigDecimal thresholdAmount = targetAmount
                    .multiply(BigDecimal.valueOf(percentage))
                    .divide(BigDecimal.valueOf(100), SimulationMathUtils.MONEY_SCALE, RoundingMode.HALF_UP);
            int elapsedMonths = estimateMonths(thresholdAmount, startingAmount, monthlyContribution, contributionFrequency);
            int cappedMonths = Math.min(elapsedMonths, estimatedMonths);
            BigDecimal projectedAmount = projectedAmountAtMonth(
                    targetAmount,
                    startingAmount,
                    monthlyContribution,
                    contributionFrequency,
                    cappedMonths,
                    estimatedMonths
            );

            milestones.add(SavingsMilestoneResponse.builder()
                    .label(percentage + "%")
                    .percentage(percentage)
                    .elapsedMonths(cappedMonths)
                    .milestoneDate(startDate.plusMonths(cappedMonths))
                    .projectedAmount(projectedAmount)
                    .remainingAmount(SimulationMathUtils.money(targetAmount.subtract(projectedAmount).max(BigDecimal.ZERO)))
                    .build());
        }
        return milestones;
    }

    private List<SavingsProjectionPointResponse> buildProjectionPoints(
            BigDecimal targetAmount,
            BigDecimal startingAmount,
            BigDecimal monthlyContribution,
            ContributionFrequency contributionFrequency,
            LocalDate startDate,
            int estimatedMonths,
            int projectionHorizon) {
        int truncatedHorizon = Math.min(projectionHorizon, MAX_PROJECTION_MONTHS);
        List<SavingsProjectionPointResponse> points = new ArrayList<>();

        for (int month = 0; month <= truncatedHorizon; month++) {
            points.add(buildProjectionPoint(
                    targetAmount,
                    startingAmount,
                    monthlyContribution,
                    contributionFrequency,
                    startDate,
                    month,
                    estimatedMonths
            ));
        }

        if (projectionHorizon > truncatedHorizon) {
            points.add(buildProjectionPoint(
                    targetAmount,
                    startingAmount,
                    monthlyContribution,
                    contributionFrequency,
                    startDate,
                    projectionHorizon,
                    estimatedMonths
            ));
        }

        return points;
    }

    private SavingsProjectionPointResponse buildProjectionPoint(
            BigDecimal targetAmount,
            BigDecimal startingAmount,
            BigDecimal monthlyContribution,
            ContributionFrequency contributionFrequency,
            LocalDate startDate,
            int month,
            int estimatedMonths) {
        BigDecimal projectedAmount = projectedAmountAtMonth(
                targetAmount,
                startingAmount,
                monthlyContribution,
                contributionFrequency,
                month,
                estimatedMonths
        );

        return SavingsProjectionPointResponse.builder()
                .monthIndex(month)
                .projectionDate(startDate.plusMonths(month))
                .projectedAmount(projectedAmount)
                .progressPercentage(SimulationMathUtils.progressPercentage(projectedAmount, targetAmount))
                .build();
    }

    private BigDecimal projectedAmountAtMonth(
            BigDecimal targetAmount,
            BigDecimal startingAmount,
            BigDecimal monthlyContribution,
            ContributionFrequency contributionFrequency,
            int month,
            int estimatedMonths) {
        int effectiveMonth = Math.min(month, estimatedMonths);
        int interval = contributionFrequency.getMonthInterval();
        int events = interval == 1 ? effectiveMonth : effectiveMonth / interval;
        BigDecimal contributionValue = contributionPerEvent(monthlyContribution, contributionFrequency)
                .multiply(BigDecimal.valueOf(events));
        BigDecimal projectedAmount = startingAmount.add(contributionValue);
        if (effectiveMonth == estimatedMonths && projectedAmount.compareTo(targetAmount) < 0) {
            projectedAmount = targetAmount;
        }
        return SimulationMathUtils.money(projectedAmount);
    }

    private BigDecimal contributionPerEvent(BigDecimal monthlyContribution, ContributionFrequency contributionFrequency) {
        if (contributionFrequency == ContributionFrequency.QUARTERLY) {
            return SimulationMathUtils.money(monthlyContribution.multiply(BigDecimal.valueOf(3)));
        }
        return SimulationMathUtils.money(monthlyContribution);
    }

    private String buildSummary(
            BigDecimal targetAmount,
            BigDecimal startingAmount,
            BigDecimal monthlyContribution,
            ContributionFrequency contributionFrequency,
            int estimatedMonths,
            LocalDate estimatedEndDate,
            LocalDate targetDate) {
        String frequencyLabel = contributionFrequency == ContributionFrequency.QUARTERLY ? "trimestriel" : "mensuel";
        String baseSummary = "Objectif de " + SimulationMathUtils.money(targetAmount).toPlainString()
                + " DT estime en " + estimatedMonths + " mois, avec une atteinte prevue le "
                + DATE_FORMATTER.format(estimatedEndDate)
                + ", a partir de " + SimulationMathUtils.money(startingAmount).toPlainString()
                + " DT et d'un rythme " + frequencyLabel
                + " base sur " + SimulationMathUtils.money(monthlyContribution).toPlainString() + " DT par mois.";

        if (targetDate == null) {
            return baseSummary;
        }

        if (estimatedEndDate.isAfter(targetDate)) {
            return baseSummary + " La date cible du " + DATE_FORMATTER.format(targetDate) + " ne serait pas tenue.";
        }

        return baseSummary + " La date cible du " + DATE_FORMATTER.format(targetDate) + " est couverte.";
    }

    private record SavingsSnapshot(
            Integer estimatedMonths,
            LocalDate estimatedEndDate,
            BigDecimal totalContributed,
            BigDecimal remainingAmount,
            List<SavingsMilestoneResponse> milestones,
            List<SavingsProjectionPointResponse> projectionPoints,
            String simulationSummary
    ) {
    }
}
