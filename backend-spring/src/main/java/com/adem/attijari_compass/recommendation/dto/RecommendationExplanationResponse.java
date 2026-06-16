package com.adem.attijari_compass.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationExplanationResponse {

    private String explanation;
    private String source;
    private boolean fallbackUsed;
}
