package com.adem.attijari_compass.recommendation.service;

import org.springframework.stereotype.Service;

@Service
public class RecommendationScoringServiceImpl implements RecommendationScoringService {

    @Override
    public double scoreRestaurantOverspending(double restaurantExpense, double averageRestaurantExpense) {
        double gapRatio = averageRestaurantExpense > 0.0
                ? (restaurantExpense - averageRestaurantExpense) / averageRestaurantExpense
                : 0.0;
        return clamp(55.0 + (gapRatio * 35.0) + normalizeAmountGap(restaurantExpense - averageRestaurantExpense, 300.0), 0.0, 100.0);
    }

    @Override
    public double scoreShoppingOverspending(double shoppingExpense, double averageShoppingExpense) {
        double gapRatio = averageShoppingExpense > 0.0
                ? (shoppingExpense - averageShoppingExpense) / averageShoppingExpense
                : 0.0;
        return clamp(45.0 + (gapRatio * 30.0) + normalizeAmountGap(shoppingExpense - averageShoppingExpense, 350.0), 0.0, 100.0);
    }

    @Override
    public double scoreSavingsRate(double savingsRate) {
        double missingRate = Math.max(0.0, 10.0 - savingsRate);
        return clamp(60.0 + (missingRate * 3.0), 0.0, 100.0);
    }

    @Override
    public double scoreGoalAcceleration(double currentMonthlyContribution, double requiredMonthlyContribution) {
        if (requiredMonthlyContribution <= 0.0) {
            return 0.0;
        }
        double gapRatio = Math.max(0.0, (requiredMonthlyContribution - currentMonthlyContribution) / requiredMonthlyContribution);
        return clamp(75.0 + (gapRatio * 25.0), 0.0, 100.0);
    }

    @Override
    public double scoreAnomaly(double anomalyAmount, double averageReferenceAmount) {
        if (anomalyAmount <= 0.0) {
            return 0.0;
        }
        double ratio = averageReferenceAmount > 0.0 ? anomalyAmount / averageReferenceAmount : 1.0;
        return clamp(50.0 + ((ratio - 1.0) * 15.0) + normalizeAmountGap(anomalyAmount - averageReferenceAmount, 500.0), 0.0, 100.0);
    }

    @Override
    public double scorePositiveFeedback(double savingsRate, double possibleSavingsPotential) {
        double bonus = Math.max(0.0, savingsRate - 10.0);
        double moderation = possibleSavingsPotential > 0.0
                ? Math.max(0.0, 15.0 - Math.min(15.0, possibleSavingsPotential / 20.0))
                : 15.0;
        return clamp(10.0 + bonus + moderation, 0.0, 35.0);
    }

    private double normalizeAmountGap(double amountGap, double divider) {
        return clamp((Math.max(0.0, amountGap) / divider) * 10.0, 0.0, 10.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, round(value)));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
