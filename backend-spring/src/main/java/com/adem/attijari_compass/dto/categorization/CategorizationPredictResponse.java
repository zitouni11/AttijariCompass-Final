package com.adem.attijari_compass.dto.categorization;

public record CategorizationPredictResponse(
        String category,
        double confidence,
        String source,
        String reason,
        String normalizedText
) {
}
