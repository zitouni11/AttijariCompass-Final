package com.adem.attijari_compass.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetTargetLevel {
    PRUDENT("Prudent", "Cadre prudent pour reduire la depense sans rupture."),
    EQUILIBRE("Equilibre", "Cadre equilibre pour maitriser la categorie sur la duree."),
    RENFORCE("Renforce", "Cadre renforce pour corriger rapidement le niveau de depense.");

    private final String label;
    private final String summary;
}
