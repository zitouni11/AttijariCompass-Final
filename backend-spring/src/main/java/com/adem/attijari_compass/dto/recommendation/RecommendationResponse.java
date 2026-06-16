package com.adem.attijari_compass.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationResponse {
    private String categorie;
    private String message;
    private String suggestion;
    private Double gainEstimeEnMois;
    private String priorite;
    private String sourceType;
}

