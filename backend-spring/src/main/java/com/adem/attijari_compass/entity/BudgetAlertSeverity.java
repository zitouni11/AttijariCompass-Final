package com.adem.attijari_compass.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetAlertSeverity {
    INFO("Information", 1),
    WARNING("Attention", 2),
    CRITICAL("Critique", 3);

    private final String label;
    private final int weight;
}
