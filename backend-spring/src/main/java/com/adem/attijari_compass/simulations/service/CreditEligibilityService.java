package com.adem.attijari_compass.simulations.service;

import com.adem.attijari_compass.simulations.dto.request.CreditCalculateRequest;
import com.adem.attijari_compass.simulations.dto.response.CreditEligibilityResponse;
import com.adem.attijari_compass.simulations.model.CreditEligibilityStatus;
import com.adem.attijari_compass.simulations.util.SimulationMathUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CreditEligibilityService {

    private static final BigDecimal RECOMMENDED_DEBT_RATIO = BigDecimal.valueOf(40);
    private static final BigDecimal WATCH_DEBT_RATIO = BigDecimal.valueOf(50);
    private static final int MAX_RECOMMENDED_DURATION_MONTHS = 360;

    public CreditEligibilityResponse analyze(
            CreditCalculateRequest request,
            BigDecimal downPayment,
            BigDecimal monthlyPayment) {
        BigDecimal income = SimulationMathUtils.positiveOrZero(request.getMonthlyIncome());
        BigDecimal charges = SimulationMathUtils.positiveOrZero(request.getExistingMonthlyCharges());
        BigDecimal capacity = SimulationMathUtils.money(
                income.multiply(BigDecimal.valueOf(0.40)).subtract(charges).max(BigDecimal.ZERO)
        );
        BigDecimal debtRatio = percentage(monthlyPayment, income);
        BigDecimal maximumFinancedAmount = maximumPrincipalForPayment(
                capacity,
                request.getAnnualInterestRate(),
                request.getDurationMonths()
        );
        BigDecimal maximumRecommendedAmount = SimulationMathUtils.money(maximumFinancedAmount.add(downPayment));
        CreditEligibilityStatus status = resolveStatus(debtRatio, capacity, income);
        Integer recommendedDuration = findRecommendedDuration(request, capacity, income);
        BigDecimal chargeReduction = recommendedChargeReduction(income, charges, monthlyPayment);
        return CreditEligibilityResponse.builder()
                .status(status)
                .realRepaymentCapacity(capacity)
                .debtRatio(debtRatio)
                .maximumRecommendedAmount(maximumRecommendedAmount)
                .message(decisionMessage(status))
                .recommended(status == CreditEligibilityStatus.ELIGIBLE)
                .recommendedDurationMonths(recommendedDuration)
                .recommendedChargeReduction(chargeReduction)
                .build();
    }

    private CreditEligibilityStatus resolveStatus(BigDecimal debtRatio, BigDecimal capacity, BigDecimal income) {
        if (income.signum() <= 0 || capacity.signum() <= 0 || debtRatio.compareTo(WATCH_DEBT_RATIO) > 0) {
            return CreditEligibilityStatus.NOT_ELIGIBLE;
        }
        return debtRatio.compareTo(RECOMMENDED_DEBT_RATIO) <= 0
                ? CreditEligibilityStatus.ELIGIBLE
                : CreditEligibilityStatus.WATCH;
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total.signum() <= 0) {
            return BigDecimal.valueOf(100).setScale(2);
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal maximumPrincipalForPayment(
            BigDecimal monthlyCapacity,
            BigDecimal annualInterestRate,
            int durationMonths) {
        if (monthlyCapacity.signum() <= 0 || durationMonths <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal monthlyRate = SimulationMathUtils.monthlyRate(annualInterestRate);
        if (monthlyRate.signum() == 0) {
            return SimulationMathUtils.money(monthlyCapacity.multiply(BigDecimal.valueOf(durationMonths)));
        }
        double rate = monthlyRate.doubleValue();
        double principal = monthlyCapacity.doubleValue()
                * (1 - Math.pow(1 + rate, -durationMonths))
                / rate;
        return SimulationMathUtils.money(BigDecimal.valueOf(principal));
    }

    private Integer findRecommendedDuration(
            CreditCalculateRequest request,
            BigDecimal capacity,
            BigDecimal income) {
        if (capacity.signum() <= 0 || income.signum() <= 0) {
            return null;
        }
        BigDecimal financedAmount = SimulationMathUtils.positiveOrZero(request.getLoanAmount())
                .subtract(SimulationMathUtils.positiveOrZero(request.getDownPayment()))
                .max(BigDecimal.ZERO);
        for (int months = request.getDurationMonths(); months <= MAX_RECOMMENDED_DURATION_MONTHS; months += 12) {
            BigDecimal payment = SimulationMathUtils.computeMonthlyPayment(
                    financedAmount, request.getAnnualInterestRate(), months
            );
            if (payment.compareTo(capacity) <= 0 && percentage(payment, income).compareTo(RECOMMENDED_DEBT_RATIO) <= 0) {
                return months;
            }
        }
        return null;
    }

    private BigDecimal recommendedChargeReduction(
            BigDecimal income,
            BigDecimal charges,
            BigDecimal monthlyPayment) {
        BigDecimal maximumCharges = income.multiply(BigDecimal.valueOf(0.40))
                .subtract(monthlyPayment)
                .max(BigDecimal.ZERO);
        return SimulationMathUtils.money(charges.subtract(maximumCharges).max(BigDecimal.ZERO));
    }

    private String decisionMessage(CreditEligibilityStatus status) {
        return switch (status) {
            case ELIGIBLE ->
                    "Votre mensualite reste dans la limite recommandee par rapport a vos revenus.";
            case WATCH ->
                    "Votre mensualite est proche de la limite recommandee. Une reduction du montant ou une duree plus longue est conseillee.";
            case NOT_ELIGIBLE ->
                    "Votre mensualite depasse largement la limite recommandee. Il est preferable de reduire le montant demande avant toute demande bancaire.";
        };
    }
}
