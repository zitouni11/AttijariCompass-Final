package com.adem.attijari_compass.dto.admin.decision;

public record TransactionSourceImpactDto(
        double currentDigitalisationRate,
        double targetDigitalisationRate,
        double requiredGain,
        String mainAction,
        String note
) {
}
