package com.adem.attijari_compass.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetTargetSource {
    RECOMMENDATION_AI("Recommendation IA"),
    MANUAL("Saisie manuelle");

    private final String label;
}
