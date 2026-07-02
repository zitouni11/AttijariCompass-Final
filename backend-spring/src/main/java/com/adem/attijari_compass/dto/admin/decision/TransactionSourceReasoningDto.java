package com.adem.attijari_compass.dto.admin.decision;

public record TransactionSourceReasoningDto(
        long cardTransactions,
        long cashTransactions,
        long bankTransferTransactions,
        long totalTransactions,
        long digitalTransactions,
        double digitalisationGap,
        String explanation
) {
}
