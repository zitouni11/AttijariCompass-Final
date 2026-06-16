package com.adem.attijari_compass.simulations.model;

public enum ContributionFrequency {
    MONTHLY(1),
    QUARTERLY(3);

    private final int monthInterval;

    ContributionFrequency(int monthInterval) {
        this.monthInterval = monthInterval;
    }

    public int getMonthInterval() {
        return monthInterval;
    }
}
