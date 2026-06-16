package com.adem.attijari_compass.recommendation.service;

public interface RecommendationScoringService {

    double scoreRestaurantOverspending(double restaurantExpense, double averageRestaurantExpense);

    double scoreShoppingOverspending(double shoppingExpense, double averageShoppingExpense);

    double scoreSavingsRate(double savingsRate);

    double scoreGoalAcceleration(double currentMonthlyContribution, double requiredMonthlyContribution);

    double scoreAnomaly(double anomalyAmount, double averageReferenceAmount);

    double scorePositiveFeedback(double savingsRate, double possibleSavingsPotential);
}
