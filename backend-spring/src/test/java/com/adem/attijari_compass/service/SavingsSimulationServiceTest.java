package com.adem.attijari_compass.service;

import com.adem.attijari_compass.simulations.dto.request.SavingsCalculateRequest;
import com.adem.attijari_compass.simulations.dto.request.SavingsCompareRequest;
import com.adem.attijari_compass.simulations.dto.request.SavingsScenarioRequest;
import com.adem.attijari_compass.simulations.dto.response.SavingsCalculateResponse;
import com.adem.attijari_compass.simulations.dto.response.SavingsCompareResponse;
import com.adem.attijari_compass.simulations.model.ContributionFrequency;
import com.adem.attijari_compass.simulations.service.SavingsSimulationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingsSimulationServiceTest {

    private final SavingsSimulationService savingsSimulationService = new SavingsSimulationService();

    @Test
    void shouldCalculateSavingsProjectionWithMilestonesAndPoints() {
        SavingsCalculateResponse response = savingsSimulationService.calculate(
                SavingsCalculateRequest.builder()
                        .targetAmount(BigDecimal.valueOf(10_000))
                        .initialAmount(BigDecimal.valueOf(1_000))
                        .monthlyContribution(BigDecimal.valueOf(500))
                        .extraContribution(BigDecimal.valueOf(500))
                        .contributionFrequency(ContributionFrequency.MONTHLY)
                        .startDate(LocalDate.of(2026, 1, 1))
                        .targetDate(LocalDate.of(2027, 6, 30))
                        .build()
        );

        assertEquals(17, response.getEstimatedMonths());
        assertEquals(LocalDate.of(2027, 6, 1), response.getEstimatedEndDate());
        assertEquals(BigDecimal.valueOf(10_000).setScale(2), response.getTotalContributed());
        assertEquals(BigDecimal.valueOf(8_500).setScale(2), response.getRemainingAmount());
        assertEquals(4, response.getMilestones().size());
        assertFalse(response.getProjectionPoints().isEmpty());
        assertTrue(response.getSimulationSummary().contains("2027-06-01"));
    }

    @Test
    void shouldCompareUpToThreeSavingsScenarios() {
        SavingsCompareResponse response = savingsSimulationService.compare(
                SavingsCompareRequest.builder()
                        .scenarios(List.of(
                                SavingsScenarioRequest.builder()
                                        .scenarioName("Plan prudent")
                                        .targetAmount(BigDecimal.valueOf(20_000))
                                        .initialAmount(BigDecimal.valueOf(2_000))
                                        .monthlyContribution(BigDecimal.valueOf(600))
                                        .extraContribution(BigDecimal.ZERO)
                                        .contributionFrequency(ContributionFrequency.MONTHLY)
                                        .startDate(LocalDate.of(2026, 4, 3))
                                        .build(),
                                SavingsScenarioRequest.builder()
                                        .scenarioName("Plan trimestriel")
                                        .targetAmount(BigDecimal.valueOf(20_000))
                                        .initialAmount(BigDecimal.valueOf(2_000))
                                        .monthlyContribution(BigDecimal.valueOf(600))
                                        .extraContribution(BigDecimal.valueOf(1_800))
                                        .contributionFrequency(ContributionFrequency.QUARTERLY)
                                        .startDate(LocalDate.of(2026, 4, 3))
                                        .build()
                        ))
                        .build()
        );

        assertEquals(2, response.getScenarios().size());
        assertEquals("Plan prudent", response.getScenarios().getFirst().getScenarioName());
        assertEquals(ContributionFrequency.MONTHLY, response.getScenarios().getFirst().getInputs().getContributionFrequency());
        assertEquals(4, response.getScenarios().get(1).getMilestones().size());
    }

    @Test
    void shouldRejectTargetDateBeforeStartDate() {
        assertThrows(IllegalArgumentException.class, () -> savingsSimulationService.calculate(
                SavingsCalculateRequest.builder()
                        .targetAmount(BigDecimal.valueOf(12_000))
                        .initialAmount(BigDecimal.ZERO)
                        .monthlyContribution(BigDecimal.valueOf(500))
                        .extraContribution(BigDecimal.ZERO)
                        .contributionFrequency(ContributionFrequency.MONTHLY)
                        .startDate(LocalDate.of(2026, 6, 1))
                        .targetDate(LocalDate.of(2026, 5, 1))
                        .build()
        ));
    }
}
