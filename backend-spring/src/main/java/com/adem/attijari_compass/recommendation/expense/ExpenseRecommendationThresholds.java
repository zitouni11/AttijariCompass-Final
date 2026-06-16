package com.adem.attijari_compass.recommendation.expense;

public final class ExpenseRecommendationThresholds {

    public static final double CATEGORY_SPIKE_RATIO = 1.15d;
    public static final double DOMINANT_CATEGORY_SHARE = 0.30d;
    public static final double MONTHLY_TOTAL_SPIKE_RATIO = 1.10d;
    public static final double FIXED_CHARGES_INCOME_RATIO = 0.65d;
    public static final int MIN_BASELINE_MONTHS = 3;

    private ExpenseRecommendationThresholds() {
    }
}
