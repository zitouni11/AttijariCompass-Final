package com.adem.attijari_compass.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialInsightDto {

    private Double totalIncome;
    private Double totalExpenses;
    private Double savingsAmount;
    private Double savingsRate;
    private Double remainingBalance;
    private Double restaurantExpense;
    private Double shoppingExpense;
    private Double transportExpense;
    private Double fixedExpense;
    private Double variableExpense;
    private Double averageRestaurant3Months;
    private Double averageShopping3Months;
    private Boolean restaurantOverspending;
    private Boolean shoppingOverspending;
    private Boolean savingsTooLow;
    private Boolean goalDelayed;
    private Boolean anomalyDetected;
    private Boolean goodFinancialDiscipline;
    private Double anomalyAmount;
    private Double requiredMonthlyContributionForGoal;
    private Double currentMonthlyContribution;
    private Double possibleSavingsPotential;
}
