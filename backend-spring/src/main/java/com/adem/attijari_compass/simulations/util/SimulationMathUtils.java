package com.adem.attijari_compass.simulations.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class SimulationMathUtils {

    public static final int MONEY_SCALE = 2;
    public static final int RATE_SCALE = 12;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private SimulationMathUtils() {
    }

    public static BigDecimal money(BigDecimal value) {
        return safe(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static BigDecimal positiveOrZero(BigDecimal value) {
        return safe(value).max(BigDecimal.ZERO);
    }

    public static LocalDate defaultStartDate(LocalDate startDate) {
        return startDate == null ? LocalDate.now() : startDate;
    }

    public static int monthsBetween(LocalDate startDate, LocalDate endDate) {
        if (endDate == null || endDate.isBefore(startDate)) {
            return 0;
        }

        long months = ChronoUnit.MONTHS.between(startDate, endDate);
        if (endDate.getDayOfMonth() > startDate.getDayOfMonth()) {
            months += 1;
        }

        return Math.toIntExact(months);
    }

    public static BigDecimal progressPercentage(BigDecimal amount, BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal progress = safe(amount)
                .multiply(HUNDRED)
                .divide(targetAmount, MONEY_SCALE, RoundingMode.HALF_UP);
        return progress.min(HUNDRED);
    }

    public static BigDecimal monthlyRate(BigDecimal annualInterestRate) {
        return safe(annualInterestRate)
                .divide(HUNDRED, RATE_SCALE, RoundingMode.HALF_UP)
                .divide(TWELVE, RATE_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal computeMonthlyPayment(BigDecimal financedAmount, BigDecimal annualInterestRate, int durationMonths) {
        if (financedAmount == null || financedAmount.compareTo(BigDecimal.ZERO) <= 0 || durationMonths <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = monthlyRate(annualInterestRate);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return financedAmount.divide(BigDecimal.valueOf(durationMonths), MONEY_SCALE, RoundingMode.HALF_UP);
        }

        double principal = financedAmount.doubleValue();
        double rate = monthlyRate.doubleValue();
        double payment = principal * rate / (1 - Math.pow(1 + rate, -durationMonths));
        return money(BigDecimal.valueOf(payment));
    }
}
