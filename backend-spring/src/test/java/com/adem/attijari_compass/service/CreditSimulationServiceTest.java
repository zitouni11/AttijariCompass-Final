package com.adem.attijari_compass.service;

import com.adem.attijari_compass.simulations.dto.request.CreditCalculateRequest;
import com.adem.attijari_compass.simulations.dto.request.CreditCompareRequest;
import com.adem.attijari_compass.simulations.dto.request.CreditScenarioRequest;
import com.adem.attijari_compass.simulations.dto.response.CreditCalculateResponse;
import com.adem.attijari_compass.simulations.dto.response.CreditCompareResponse;
import com.adem.attijari_compass.simulations.service.CreditSimulationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditSimulationServiceTest {

    private final CreditSimulationService creditSimulationService = new CreditSimulationService();

    @Test
    void shouldCalculateCreditProjectionAndEarlyRepaymentImpact() {
        CreditCalculateResponse response = creditSimulationService.calculate(
                CreditCalculateRequest.builder()
                        .loanAmount(BigDecimal.valueOf(220_000))
                        .downPayment(BigDecimal.valueOf(30_000))
                        .annualInterestRate(BigDecimal.valueOf(7.1))
                        .durationMonths(84)
                        .monthlyIncome(BigDecimal.valueOf(6_200))
                        .earlyRepaymentAmount(BigDecimal.valueOf(15_000))
                        .earlyRepaymentMonth(24)
                        .build()
        );

        assertEquals(BigDecimal.valueOf(190_000).setScale(2), response.getFinancedAmount());
        assertTrue(response.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getTotalInterest().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(response.getEndDate());
        assertFalse(response.getAmortizationPreview().isEmpty());
        assertNotNull(response.getEarlyRepaymentImpact());
        assertTrue(response.getEarlyRepaymentImpact().getInterestSaved().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldCompareCreditScenarios() {
        CreditCompareResponse response = creditSimulationService.compare(
                CreditCompareRequest.builder()
                        .scenarios(List.of(
                                CreditScenarioRequest.builder()
                                        .scenarioName("60 mois")
                                        .loanAmount(BigDecimal.valueOf(180_000))
                                        .downPayment(BigDecimal.valueOf(20_000))
                                        .annualInterestRate(BigDecimal.valueOf(6.5))
                                        .durationMonths(60)
                                        .monthlyIncome(BigDecimal.valueOf(5_000))
                                        .build(),
                                CreditScenarioRequest.builder()
                                        .scenarioName("84 mois")
                                        .loanAmount(BigDecimal.valueOf(180_000))
                                        .downPayment(BigDecimal.valueOf(20_000))
                                        .annualInterestRate(BigDecimal.valueOf(6.5))
                                        .durationMonths(84)
                                        .monthlyIncome(BigDecimal.valueOf(5_000))
                                        .build()
                        ))
                        .build()
        );

        assertEquals(2, response.getScenarios().size());
        assertEquals("60 mois", response.getScenarios().getFirst().getScenarioName());
        assertTrue(response.getScenarios().getFirst().getMonthlyPayment()
                .compareTo(response.getScenarios().get(1).getMonthlyPayment()) > 0);
    }

    @Test
    void shouldRejectDownPaymentGreaterThanLoanAmount() {
        assertThrows(IllegalArgumentException.class, () -> creditSimulationService.calculate(
                CreditCalculateRequest.builder()
                        .loanAmount(BigDecimal.valueOf(50_000))
                        .downPayment(BigDecimal.valueOf(60_000))
                        .annualInterestRate(BigDecimal.valueOf(6))
                        .durationMonths(48)
                        .build()
        ));
    }
}
