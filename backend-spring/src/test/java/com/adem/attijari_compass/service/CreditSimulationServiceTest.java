package com.adem.attijari_compass.service;

import com.adem.attijari_compass.simulations.dto.request.CreditCalculateRequest;
import com.adem.attijari_compass.simulations.dto.request.CreditCompareRequest;
import com.adem.attijari_compass.simulations.dto.request.CreditScenarioRequest;
import com.adem.attijari_compass.simulations.dto.response.CreditCalculateResponse;
import com.adem.attijari_compass.simulations.dto.response.CreditCompareResponse;
import com.adem.attijari_compass.simulations.model.CreditEligibilityStatus;
import com.adem.attijari_compass.simulations.service.CreditSimulationService;
import com.adem.attijari_compass.simulations.service.CreditEligibilityService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditSimulationServiceTest {

    private final CreditEligibilityService creditEligibilityService = new CreditEligibilityService();
    private final CreditSimulationService creditSimulationService = new CreditSimulationService(creditEligibilityService);

    @Test
    void shouldCalculateCreditProjectionAndEarlyRepaymentImpact() {
        CreditCalculateResponse response = creditSimulationService.calculate(
                CreditCalculateRequest.builder()
                        .loanAmount(BigDecimal.valueOf(220_000))
                        .downPayment(BigDecimal.valueOf(30_000))
                        .annualInterestRate(BigDecimal.valueOf(7.1))
                        .durationMonths(84)
                        .monthlyIncome(BigDecimal.valueOf(6_200))
                        .existingMonthlyCharges(BigDecimal.ZERO)
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
                                        .existingMonthlyCharges(BigDecimal.ZERO)
                                        .build(),
                                CreditScenarioRequest.builder()
                                        .scenarioName("84 mois")
                                        .loanAmount(BigDecimal.valueOf(180_000))
                                        .downPayment(BigDecimal.valueOf(20_000))
                                        .annualInterestRate(BigDecimal.valueOf(6.5))
                                        .durationMonths(84)
                                        .monthlyIncome(BigDecimal.valueOf(5_000))
                                        .existingMonthlyCharges(BigDecimal.ZERO)
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

    @Test
    void shouldFlagRequestedReferenceCasesWithConsistentEligibility() {
        CreditCalculateResponse watchCase = calculateEligibilityCase(220_000, 30_000, 84, 6_200, 0);
        CreditCalculateResponse rejectedCase = calculateEligibilityCase(500_000, 30_000, 84, 3_000, 800);
        CreditCalculateResponse eligibleCase = calculateEligibilityCase(80_000, 30_000, 60, 5_000, 500);

        assertEquals(CreditEligibilityStatus.WATCH, watchCase.getEligibility().getStatus());
        assertEquals(CreditEligibilityStatus.NOT_ELIGIBLE, rejectedCase.getEligibility().getStatus());
        assertEquals(CreditEligibilityStatus.ELIGIBLE, eligibleCase.getEligibility().getStatus());

        assertEquals(BigDecimal.valueOf(2_480).setScale(2), watchCase.getEligibility().getRealRepaymentCapacity());
        assertEquals(BigDecimal.valueOf(46.40).setScale(2), watchCase.getEligibility().getDebtRatio());
        assertEquals(BigDecimal.valueOf(193_787.06).setScale(2), watchCase.getEligibility().getMaximumRecommendedAmount());
        assertEquals(BigDecimal.valueOf(400).setScale(2), rejectedCase.getEligibility().getRealRepaymentCapacity());
        assertEquals(BigDecimal.valueOf(1_500).setScale(2), eligibleCase.getEligibility().getRealRepaymentCapacity());
        assertTrue(rejectedCase.getEligibility().getMaximumRecommendedAmount()
                .compareTo(BigDecimal.valueOf(500_000)) < 0);
        assertFalse(rejectedCase.getEligibility().isRecommended());
    }

    private CreditCalculateResponse calculateEligibilityCase(
            long amount,
            long downPayment,
            int durationMonths,
            long income,
            long charges) {
        return creditSimulationService.calculate(
                CreditCalculateRequest.builder()
                        .loanAmount(BigDecimal.valueOf(amount))
                        .downPayment(BigDecimal.valueOf(downPayment))
                        .annualInterestRate(BigDecimal.valueOf(7.1))
                        .durationMonths(durationMonths)
                        .monthlyIncome(BigDecimal.valueOf(income))
                        .existingMonthlyCharges(BigDecimal.valueOf(charges))
                        .build()
        );
    }
}
