package com.adem.attijari_compass.dto.categorization;

import jakarta.validation.constraints.Size;

public record CategorizationPredictRequest(
        @Size(max = 255, message = "merchantName must be <= 255 characters")
        String merchantName,

        @Size(max = 1000, message = "description must be <= 1000 characters")
        String description
) {
}
