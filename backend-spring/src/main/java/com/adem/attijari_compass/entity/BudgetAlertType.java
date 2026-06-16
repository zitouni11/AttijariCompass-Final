package com.adem.attijari_compass.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetAlertType {
    BUDGET_DEPASSE("Budget depasse"),
    BUDGET_QUASI_ATTEINT("Budget presque atteint"),
    BUDGET_RESTE_FAIBLE("Reste faible"),
    BUDGET_SOUS_CONTROLE("Budget sous controle"),
    BUDGET_CRITIQUE_PRIORITAIRE("Budget critique prioritaire"),
    BUDGET_MAITRISE_GLOBALE("Bonne maitrise budgetaire");

    private final String label;
}
