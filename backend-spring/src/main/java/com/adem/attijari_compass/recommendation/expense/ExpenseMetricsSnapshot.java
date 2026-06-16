package com.adem.attijari_compass.recommendation.expense;

import com.adem.attijari_compass.entity.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseMetricsSnapshot {

    @Builder.Default
    private int analysisWindowDays = 0;

    @Builder.Default
    private LocalDate analysisStartDate = null;

    @Builder.Default
    private LocalDate analysisEndDate = null;

    @Builder.Default
    private BigDecimal analysisExpenseTotal = BigDecimal.ZERO;

    @Builder.Default
    private Map<TransactionCategory, BigDecimal> analysisCategoryTotals = new EnumMap<>(TransactionCategory.class);

    @Builder.Default
    private Map<TransactionCategory, Long> analysisCategoryCounts = new EnumMap<>(TransactionCategory.class);

    @Builder.Default
    private BigDecimal baselineMonthlyAverage = BigDecimal.ZERO;

    @Builder.Default
    private Map<TransactionCategory, BigDecimal> baselineCategoryAverages = new EnumMap<>(TransactionCategory.class);

    @Builder.Default
    private BigDecimal fixedChargesTotal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal analysisIncomeTotal = BigDecimal.ZERO;
}
