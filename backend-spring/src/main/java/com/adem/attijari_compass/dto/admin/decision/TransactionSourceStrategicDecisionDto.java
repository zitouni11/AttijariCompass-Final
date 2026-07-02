package com.adem.attijari_compass.dto.admin.decision;

public record TransactionSourceStrategicDecisionDto(
        String objectif,
        String levierPrincipal,
        String levierSecondaire,
        String justification,
        String decisionRecommandee,
        long transactionsDigitalesNecessaires,
        long transactionsAConvertir,
        String impactAttendu
) {
}
