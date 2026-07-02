package com.adem.attijari_compass.dto.admin.decision;

public record TransactionSourceMetricDto(
        String source,
        long transactionCount,
        double percentage,
        double totalAmount,
        double averageAmount
) {
}
