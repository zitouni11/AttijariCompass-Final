package com.adem.attijari_compass.dto.admin.decision;

public record TransactionSourceActionPlanItemDto(
        String priorite,
        String action,
        String justification,
        String impactAttendu,
        String difficulte
) {
}
