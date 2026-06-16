package com.adem.attijari_compass.dto.categorization;

public record MlPredictionResponse(
        String category,
        double confidence,
        String normalizedText
) {
}
