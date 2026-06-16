package com.adem.attijari_compass.simulations.service;

import com.adem.attijari_compass.simulations.dto.request.CreditCalculateRequest;
import com.adem.attijari_compass.simulations.dto.request.CreditCompareRequest;
import com.adem.attijari_compass.simulations.dto.request.CreditScenarioRequest;
import com.adem.attijari_compass.simulations.dto.response.AmortizationPreviewItemResponse;
import com.adem.attijari_compass.simulations.dto.response.CreditCalculateResponse;
import com.adem.attijari_compass.simulations.dto.response.CreditCompareResponse;
import com.adem.attijari_compass.simulations.dto.response.CreditScenarioResult;
import com.adem.attijari_compass.simulations.dto.response.EarlyRepaymentImpactResponse;
import com.adem.attijari_compass.simulations.util.SimulationMathUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CreditSimulationService {

    private static final int PREVIEW_INSTALLMENT_COUNT = 12;

    public CreditCalculateResponse calculate(CreditCalculateRequest request) {
        CreditSnapshot snapshot = calculateSnapshot(request);
        return CreditCalculateResponse.builder()
                .financedAmount(snapshot.financedAmount())
                .monthlyPayment(snapshot.monthlyPayment())
                .totalCost(snapshot.totalCost())
                .totalInterest(snapshot.totalInterest())
                .endDate(snapshot.endDate())
                .amortizationPreview(snapshot.amortizationPreview())
                .earlyRepaymentImpact(snapshot.earlyRepaymentImpact())
                .build();
    }

    public CreditCompareResponse compare(CreditCompareRequest request) {
        List<CreditScenarioResult> scenarios = request.getScenarios().stream()
                .map(this::calculateScenario)
                .toList();

        return CreditCompareResponse.builder()
                .scenarios(scenarios)
                .build();
    }

    private CreditScenarioResult calculateScenario(CreditScenarioRequest request) {
        CreditSnapshot snapshot = calculateSnapshot(CreditCalculateRequest.builder()
                .loanAmount(request.getLoanAmount())
                .downPayment(request.getDownPayment())
                .annualInterestRate(request.getAnnualInterestRate())
                .durationMonths(request.getDurationMonths())
                .monthlyIncome(request.getMonthlyIncome())
                .earlyRepaymentAmount(request.getEarlyRepaymentAmount())
                .earlyRepaymentMonth(request.getEarlyRepaymentMonth())
                .build());

        return CreditScenarioResult.builder()
                .scenarioName(request.getScenarioName())
                .durationMonths(request.getDurationMonths())
                .monthlyPayment(snapshot.monthlyPayment())
                .totalCost(snapshot.totalCost())
                .totalInterest(snapshot.totalInterest())
                .endDate(snapshot.endDate())
                .build();
    }

    private CreditSnapshot calculateSnapshot(CreditCalculateRequest request) {
        validateRequest(request);

        BigDecimal loanAmount = SimulationMathUtils.money(request.getLoanAmount());
        BigDecimal downPayment = SimulationMathUtils.money(request.getDownPayment());
        BigDecimal financedAmount = SimulationMathUtils.money(loanAmount.subtract(downPayment).max(BigDecimal.ZERO));
        BigDecimal monthlyPayment = SimulationMathUtils.computeMonthlyPayment(
                financedAmount,
                request.getAnnualInterestRate(),
                request.getDurationMonths()
        );
        LocalDate startDate = LocalDate.now();

        List<Installment> baselineSchedule = buildSchedule(
                financedAmount,
                request.getAnnualInterestRate(),
                monthlyPayment,
                request.getDurationMonths(),
                startDate,
                null,
                null
        );
        List<Installment> scenarioSchedule = buildSchedule(
                financedAmount,
                request.getAnnualInterestRate(),
                monthlyPayment,
                request.getDurationMonths(),
                startDate,
                request.getEarlyRepaymentAmount(),
                request.getEarlyRepaymentMonth()
        );

        BigDecimal baselineTotalInterest = totalInterest(baselineSchedule);
        BigDecimal scenarioTotalInterest = totalInterest(scenarioSchedule);
        BigDecimal scenarioInstallmentCost = totalPayment(scenarioSchedule);
        LocalDate endDate = scenarioSchedule.isEmpty() ? startDate : scenarioSchedule.getLast().paymentDate();

        return new CreditSnapshot(
                financedAmount,
                monthlyPayment,
                SimulationMathUtils.money(downPayment.add(scenarioInstallmentCost)),
                scenarioTotalInterest,
                endDate,
                buildPreview(scenarioSchedule),
                buildEarlyRepaymentImpact(
                        request.getEarlyRepaymentAmount(),
                        request.getEarlyRepaymentMonth(),
                        baselineSchedule,
                        scenarioSchedule,
                        baselineTotalInterest,
                        scenarioTotalInterest
                )
        );
    }

    private void validateRequest(CreditCalculateRequest request) {
        BigDecimal loanAmount = SimulationMathUtils.positiveOrZero(request.getLoanAmount());
        BigDecimal downPayment = SimulationMathUtils.positiveOrZero(request.getDownPayment());
        if (downPayment.compareTo(loanAmount) > 0) {
            throw new IllegalArgumentException("downPayment cannot exceed loanAmount");
        }

        boolean hasEarlyAmount = request.getEarlyRepaymentAmount() != null
                && request.getEarlyRepaymentAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean hasEarlyMonth = request.getEarlyRepaymentMonth() != null;
        if (hasEarlyAmount != hasEarlyMonth) {
            throw new IllegalArgumentException("earlyRepaymentAmount and earlyRepaymentMonth must be provided together");
        }

        if (hasEarlyMonth && request.getEarlyRepaymentMonth() > request.getDurationMonths()) {
            throw new IllegalArgumentException("earlyRepaymentMonth cannot exceed durationMonths");
        }
    }

    private List<Installment> buildSchedule(
            BigDecimal financedAmount,
            BigDecimal annualInterestRate,
            BigDecimal monthlyPayment,
            int durationMonths,
            LocalDate startDate,
            BigDecimal earlyRepaymentAmount,
            Integer earlyRepaymentMonth) {
        if (financedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        BigDecimal monthlyRate = SimulationMathUtils.monthlyRate(annualInterestRate);
        List<Installment> installments = new ArrayList<>();
        BigDecimal balance = financedAmount;
        int monthNumber = 1;

        while (balance.compareTo(BigDecimal.ZERO) > 0 && monthNumber <= durationMonths + 360) {
            BigDecimal openingBalance = balance;
            BigDecimal interestPaid = SimulationMathUtils.money(openingBalance.multiply(monthlyRate));
            BigDecimal scheduledPrincipal = monthlyPayment.subtract(interestPaid);
            if (scheduledPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                scheduledPrincipal = BigDecimal.ZERO;
            }
            if (scheduledPrincipal.compareTo(openingBalance) > 0) {
                scheduledPrincipal = openingBalance;
            }

            BigDecimal extraPrincipal = BigDecimal.ZERO;
            if (earlyRepaymentMonth != null && monthNumber == earlyRepaymentMonth) {
                BigDecimal maxExtra = openingBalance.subtract(scheduledPrincipal).max(BigDecimal.ZERO);
                extraPrincipal = SimulationMathUtils.money(SimulationMathUtils.positiveOrZero(earlyRepaymentAmount).min(maxExtra));
            }

            BigDecimal principalPaid = SimulationMathUtils.money(scheduledPrincipal.add(extraPrincipal));
            BigDecimal totalPayment = SimulationMathUtils.money(principalPaid.add(interestPaid));
            BigDecimal closingBalance = SimulationMathUtils.money(openingBalance.subtract(principalPaid).max(BigDecimal.ZERO));

            installments.add(new Installment(
                    monthNumber,
                    startDate.plusMonths(monthNumber),
                    SimulationMathUtils.money(openingBalance),
                    SimulationMathUtils.money(principalPaid),
                    interestPaid,
                    totalPayment,
                    closingBalance
            ));

            balance = closingBalance;
            monthNumber++;
        }

        return installments;
    }

    private List<AmortizationPreviewItemResponse> buildPreview(List<Installment> schedule) {
        if (schedule.isEmpty()) {
            return List.of();
        }

        List<AmortizationPreviewItemResponse> preview = new ArrayList<>();
        int previewCount = Math.min(PREVIEW_INSTALLMENT_COUNT, schedule.size());
        for (int index = 0; index < previewCount; index++) {
            preview.add(toPreview(schedule.get(index)));
        }

        Installment lastInstallment = schedule.getLast();
        if (lastInstallment.monthNumber() > previewCount) {
            preview.add(toPreview(lastInstallment));
        }

        return preview;
    }

    private AmortizationPreviewItemResponse toPreview(Installment installment) {
        return AmortizationPreviewItemResponse.builder()
                .monthNumber(installment.monthNumber())
                .paymentDate(installment.paymentDate())
                .openingBalance(installment.openingBalance())
                .principalPaid(installment.principalPaid())
                .interestPaid(installment.interestPaid())
                .totalPayment(installment.totalPayment())
                .closingBalance(installment.closingBalance())
                .build();
    }

    private EarlyRepaymentImpactResponse buildEarlyRepaymentImpact(
            BigDecimal earlyRepaymentAmount,
            Integer earlyRepaymentMonth,
            List<Installment> baselineSchedule,
            List<Installment> scenarioSchedule,
            BigDecimal baselineTotalInterest,
            BigDecimal scenarioTotalInterest) {
        if (earlyRepaymentAmount == null || earlyRepaymentAmount.compareTo(BigDecimal.ZERO) <= 0 || earlyRepaymentMonth == null) {
            return null;
        }

        LocalDate originalEndDate = baselineSchedule.isEmpty() ? LocalDate.now() : baselineSchedule.getLast().paymentDate();
        LocalDate updatedEndDate = scenarioSchedule.isEmpty() ? LocalDate.now() : scenarioSchedule.getLast().paymentDate();

        return EarlyRepaymentImpactResponse.builder()
                .appliedAmount(SimulationMathUtils.money(earlyRepaymentAmount))
                .appliedMonth(earlyRepaymentMonth)
                .originalEndDate(originalEndDate)
                .updatedEndDate(updatedEndDate)
                .monthsSaved(Math.max(0, baselineSchedule.size() - scenarioSchedule.size()))
                .originalTotalInterest(baselineTotalInterest)
                .updatedTotalInterest(scenarioTotalInterest)
                .interestSaved(SimulationMathUtils.money(baselineTotalInterest.subtract(scenarioTotalInterest).max(BigDecimal.ZERO)))
                .build();
    }

    private BigDecimal totalPayment(List<Installment> schedule) {
        return SimulationMathUtils.money(schedule.stream()
                .map(Installment::totalPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal totalInterest(List<Installment> schedule) {
        return SimulationMathUtils.money(schedule.stream()
                .map(Installment::interestPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private record Installment(
            int monthNumber,
            LocalDate paymentDate,
            BigDecimal openingBalance,
            BigDecimal principalPaid,
            BigDecimal interestPaid,
            BigDecimal totalPayment,
            BigDecimal closingBalance
    ) {
    }

    private record CreditSnapshot(
            BigDecimal financedAmount,
            BigDecimal monthlyPayment,
            BigDecimal totalCost,
            BigDecimal totalInterest,
            LocalDate endDate,
            List<AmortizationPreviewItemResponse> amortizationPreview,
            EarlyRepaymentImpactResponse earlyRepaymentImpact
    ) {
    }
}
