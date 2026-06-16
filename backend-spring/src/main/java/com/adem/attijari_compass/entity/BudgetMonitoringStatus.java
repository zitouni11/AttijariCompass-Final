package com.adem.attijari_compass.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetMonitoringStatus {
    SOUS_CONTROLE("Sous controle"),
    A_SURVEILLER("A surveiller"),
    DEPASSE("Depasse");

    private final String label;
}
