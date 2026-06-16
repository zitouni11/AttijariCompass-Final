package com.adem.attijari_compass.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetTargetStatus {
    ACTIVE("Actif"),
    INACTIVE("Inactif"),
    ARCHIVED("Archive");

    private final String label;
}
